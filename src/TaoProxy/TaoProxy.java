package TaoProxy;

import Configuration.TaoConfigs;

import Messages.*;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.Executors;

/**
 * @brief Class that represents the proxy which handles requests from clients and replies from servers
 */
public class TaoProxy implements Proxy {
    // Sequencer for proxy
    private Sequencer mSequencer;

    // Processor for proxy
    private Processor mProcessor;

    // Thread group for asynchronous sockets
    private AsynchronousChannelGroup mThreadGroup;

    // A MessageCreator to create different types of messages to be passed from client, proxy, and server
    private MessageCreator mMessageCreator;

    // A PathCreator
    private PathCreator mPathCreator;

    // A CryptoUtil
    private CryptoUtil mCryptoUtil;

    //
    private Subtree mSubtree;

    /**
     * @brief Constructor
     * @param minServerSize
     * @param messageCreator
     * @param pathCreator
     */
    public TaoProxy(long minServerSize, MessageCreator messageCreator, PathCreator pathCreator, Subtree subtree) {
        try {
            // TODO: Remove this, for trace only
            TaoLogger.logOn = false;

            // Initialize needed constants
            TaoConfigs.initConfiguration(minServerSize);

            // Create a CryptoUtil
            mCryptoUtil = new TaoCryptoUtil();
            mSubtree = subtree;

            // Assign the message and path creators
            mMessageCreator = messageCreator;
            mPathCreator = pathCreator;

            // Create a thread pool for asynchronous sockets
            mThreadGroup = AsynchronousChannelGroup.withFixedThreadPool(TaoConfigs.PROXY_THREAD_COUNT, Executors.defaultThreadFactory());

            // Initialize the sequencer and proxy
            mSequencer = new TaoSequencer(mMessageCreator, mPathCreator);
            mProcessor = new TaoProcessor(this, mSequencer, mThreadGroup, mMessageCreator, mPathCreator, mCryptoUtil, mSubtree);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @brief Function to initialize an empty tree on the server side
     */
    public void initializeServer() {
        try {
            // TODO: Write the top of subtree for multiple storage partition
            //   mSubtree.initRoot();

            // Get the total number of paths
            int totalPaths = 1 << TaoConfigs.TREE_HEIGHT;
            TaoLogger.log("Total paths " + totalPaths);

            // Variables to both hold the data of a path as well as how big the path is
            byte[] dataToWrite;
            int pathSize;

            // Loop to write each path to server
            for (int i = 0; i < totalPaths; i++) {
                TaoLogger.log("Creating path " + i);

                // Connect to server, create input and output streams
                Socket socket = new Socket(TaoConfigs.SERVER_HOSTNAME, TaoConfigs.SERVER_PORT);
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                InputStream input = socket.getInputStream();

                // Create empty paths and serialize
                Path defaultPath = mPathCreator.createPath();
                defaultPath.setPathID(i);
                dataToWrite = mCryptoUtil.encryptPath(defaultPath);
                pathSize = dataToWrite.length;

                // Create a proxy write request
                ProxyRequest writebackRequest = mMessageCreator.createProxyRequest();
                writebackRequest.setType(MessageTypes.PROXY_WRITE_REQUEST);
                writebackRequest.setPathSize(pathSize);
                writebackRequest.setDataToWrite(dataToWrite);

                // Serialize the proxy request
                byte[] proxyRequest = writebackRequest.serialize();

                // Send the type and size of message to server
                byte[] messageTypeBytes = Ints.toByteArray(MessageTypes.PROXY_WRITE_REQUEST);
                byte[] messageLengthBytes = Ints.toByteArray(proxyRequest.length);
                output.write(Bytes.concat(messageTypeBytes, messageLengthBytes));

                // Send actual message to server
                output.write(proxyRequest);

                // Read in the response
                // TODO: Currently not doing anything with response, possibly do something
                byte[] typeAndSize = new byte[8];
                input.read(typeAndSize);
                int type = Ints.fromByteArray(Arrays.copyOfRange(typeAndSize, 0, 4));
                int length = Ints.fromByteArray(Arrays.copyOfRange(typeAndSize, 4, 8));
                byte[] message = new byte[length];
                input.read(message);

                // Close socket and streams
                output.close();
                input.close();
                socket.close();
            }

            TaoLogger.log("Finished init, running proxy");
            // Run the proxy now that the server is initialized
            run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveRequest(ClientRequest req) {
        // When we receive a request, we first send it to the sequencer
        mSequencer.onReceiveRequest(req);

        // We then send the request to the processor, starting with the read path method
        mProcessor.readPath(req);
    }

    @Override
    public void onReceiveResponse(ClientRequest req, ServerResponse resp, boolean isFakeRead) {
        // When a response is received, the processor will answer the request, flush the path, then may perform a
        // write back
        mProcessor.answerRequest(req, resp, isFakeRead);
        mProcessor.flush(resp.getPathID());
        mProcessor.writeBack(TaoConfigs.WRITE_BACK_THRESHOLD);
    }

    @Override
    public void run() {
        try {
            // Create an asynchronous channel to listen for connections
            AsynchronousServerSocketChannel channel =
                    AsynchronousServerSocketChannel.open(mThreadGroup).bind(new InetSocketAddress(TaoConfigs.PROXY_PORT));

            // Asynchronously wait for incoming connections
            channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                @Override
                public void completed(AsynchronousSocketChannel ch, Void att) {
                    // Start listening for other connections
                    channel.accept(null, this);

                    // Create a ByteBuffer to read in message type
                    ByteBuffer typeByteBuffer = MessageUtility.createTypeReceiveBuffer();

                    // Asynchronously read message
                    ch.read(typeByteBuffer, null, new CompletionHandler<Integer, Void>() {
                        @Override
                        public void completed(Integer result, Void attachment) {
                            // Flip the byte buffer for reading
                            typeByteBuffer.flip();

                            // Figure out the type of the message
                            int[] typeAndLength = MessageUtility.parseTypeAndLength(typeByteBuffer);
                            int messageType = typeAndLength[0];
                            int messageLength = typeAndLength[1];

                            // Serve message based on type
                            if (messageType == MessageTypes.CLIENT_WRITE_REQUEST || messageType == MessageTypes.CLIENT_READ_REQUEST) {
                                // Get the rest of the message
                                ByteBuffer messageByteBuffer = ByteBuffer.allocate(messageLength);

                                // Do one last asynchronous read to get the rest of the message
                                ch.read(messageByteBuffer, null, new CompletionHandler<Integer, Void>() {
                                    @Override
                                    public void completed(Integer result, Void attachment) {
                                        // Make sure we read all the bytes
                                        while (messageByteBuffer.remaining() > 0) {
                                            ch.read(messageByteBuffer, null, this);
                                            return;
                                        }

                                        // Flip the byte buffer for reading
                                        messageByteBuffer.flip();

                                        // Get the rest of the bytes for the message
                                        byte[] requestBytes = new byte[messageLength];
                                        messageByteBuffer.get(requestBytes);

                                        // Create ClientRequest object based on read bytes
                                        ClientRequest clientReq = mMessageCreator.createClientRequest();
                                        clientReq.initFromSerialized(requestBytes);

                                        // Handle request
                                        onReceiveRequest(clientReq);
                                    }

                                    @Override
                                    public void failed(Throwable exc, Void attachment) {
                                        // TODO: implement?
                                    }
                                });

                            }
                        }
                        @Override
                        public void failed(Throwable exc, Void attachment) {
                            // TODO: implement?
                        }
                    });
                }
                @Override
                public void failed(Throwable exc, Void att) {
                    // TODO: implement?
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Create proxy and run
        TaoProxy proxy = new TaoProxy(Long.parseLong(args[0]), new TaoMessageCreator(), new TaoBlockCreator(), new TaoSubtree());
        proxy.run();
    }
}
