package TaoProxyTest;

import Configuration.TaoConfigs;
import Messages.ClientRequest;
import Messages.MessageCreator;
import Messages.ProxyRequest;
import TaoProxy.*;
import org.junit.Test;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 *
 */
public class TaoMessageCreatorTest {
    @Test
    public void testClientRequest() {
        MessageCreator messageCreator = new TaoMessageCreator();
        ClientRequest clientRequest = messageCreator.createClientRequest();
        clientRequest.setBlockID(3);
        clientRequest.setType(Constants.CLIENT_READ_REQUEST);
        clientRequest.setRequestID(6);
        byte[] dataToWrite = new byte[Constants.BLOCK_SIZE];
        Arrays.fill(dataToWrite, (byte) 1);
        clientRequest.setData(dataToWrite);

        ClientRequest newClientRequest = messageCreator.parseClientRequestBytes(clientRequest.serialize());
        assertEquals(clientRequest.getBlockID(), newClientRequest.getBlockID());
        assertEquals(clientRequest.getType(), newClientRequest.getType());
        assertEquals(clientRequest.getRequestID(), newClientRequest.getRequestID());
        assertTrue(Arrays.equals(clientRequest.getData(), newClientRequest.getData()));
    }

    @Test
    public void testProxyRequest() {
        MessageCreator messageCreator = new TaoMessageCreator();
        ProxyRequest proxyRequest = messageCreator.createProxyRequest();
        proxyRequest.setType(Constants.PROXY_READ_REQUEST);
        proxyRequest.setPathSize(10);
        proxyRequest.setPathID(1);

        ProxyRequest newProxyRequest = messageCreator.parseProxyRequestBytes(proxyRequest.serialize());
        assertEquals(proxyRequest.getType(), newProxyRequest.getType());
     //   assertEquals(proxyRequest.getPathSize(), newProxyRequest.getPathSize());
        assertEquals(proxyRequest.getPathID(), newProxyRequest.getPathID());

    }

    @Test
    public void testProxyWriteRequest() {
        long systemSize = 246420;
        TaoConfigs.initConfiguration(systemSize);

        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyGen.init(128);
        CryptoUtil cryptoUtil = new TaoCryptoUtil(keyGen.generateKey());

        PathCreator pathCreator = new TaoBlockCreator();
        MessageCreator messageCreator = new TaoMessageCreator();

        // Create empty path
        long pathID = 9;
        TaoPath testPath = new TaoPath(pathID);

        // Create empty buckets
        TaoBucket[] testBuckets = new TaoBucket[TaoConfigs.TREE_HEIGHT + 1];

        // Fill in each bucket
        for (int i = 0; i < testBuckets.length; i++) {
            // Create blocks for bucket
            TaoBlock[] testBlocks = new TaoBlock[TaoConfigs.BLOCKS_IN_BUCKET];
            byte[] bytes = new byte[TaoConfigs.BLOCK_SIZE];

            testBuckets[i] = new TaoBucket();

            for (int j = 0; j < testBlocks.length; j++) {
                int blockID = Integer.parseInt(Integer.toString(i) + Integer.toString(j));
                testBlocks[j] = new TaoBlock(blockID);
                Arrays.fill(bytes, (byte) blockID);
                testBlocks[j].setData(bytes);

                testBuckets[i].addBlock(testBlocks[j], 1);
            }

            testPath.addBucket(testBuckets[i]);
        }

        // Create empty paths and serialize
        Path defaultPath = pathCreator.createPath();
        defaultPath.setPathID(9);
        byte[] dataToWrite = cryptoUtil.encryptPath(testPath);
        int pathSize = dataToWrite.length;

        // Create a proxy write request
        ProxyRequest writebackRequest = messageCreator.createProxyRequest();
        writebackRequest.setType(Constants.PROXY_WRITE_REQUEST);
        writebackRequest.setPathSize(pathSize);
        writebackRequest.setDataToWrite(dataToWrite);

        byte[] serialized = writebackRequest.serialize();

        ProxyRequest fromSerialized = messageCreator.parseProxyRequestBytes(serialized);
        assertEquals(writebackRequest.getType(), fromSerialized.getType());
        assertEquals(writebackRequest.getPathSize(), fromSerialized.getPathSize());
        assertTrue(Arrays.equals(writebackRequest.getDataToWrite(), fromSerialized.getDataToWrite()));
    }

    @Test
    public void testProxyResponse() {
        MessageCreator messageCreator = new TaoMessageCreator();

    }

    @Test
    public void testServerResponse() {
        MessageCreator messageCreator = new TaoMessageCreator();

    }
}