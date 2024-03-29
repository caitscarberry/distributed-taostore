package TaoProxy;

import Messages.ClientRequest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


public class TaoProfiler implements Profiler {

    protected String mOutputDirectory;

    protected DescriptiveStatistics mReadPathStatistics;
    protected DescriptiveStatistics mWriteBackStatistics;

    protected DescriptiveStatistics mReadPathSendToRecvStatistics;
    protected DescriptiveStatistics mWriteBackSendToRecvStatistics;

    protected DescriptiveStatistics mReadPathServerStatistics;
    protected DescriptiveStatistics mWriteBackServerStatistics;

    protected DescriptiveStatistics mReadPathNetStatistics;
    protected DescriptiveStatistics mWriteBackNetStatistics;

    protected DescriptiveStatistics mAddPathStatistics;

    protected Map<ClientRequest, Long> mReadPathStartTimes;
    protected Map<Long, Long> mWriteBackStartTimes;

    protected Map<InetSocketAddress, Map<ClientRequest, Long>> mReadPathPreSendTimes;
    protected Map<InetSocketAddress, Map<Long, Long>> mWriteBackPreSendTimes;

    protected Map<InetSocketAddress, Map<ClientRequest, Long>> mReadPathSendToRecvTimes;
    protected Map<InetSocketAddress, Map<Long, Long>> mWriteBackSendToRecvTimes;

    protected int unitID;

    public TaoProfiler(int uID) {
        unitID = uID;

        mOutputDirectory = "profile";

        mReadPathStatistics = new DescriptiveStatistics();
        mWriteBackStatistics = new DescriptiveStatistics();

        mReadPathSendToRecvStatistics = new DescriptiveStatistics();
        mWriteBackSendToRecvStatistics = new DescriptiveStatistics();

        mReadPathServerStatistics = new DescriptiveStatistics();
        mWriteBackServerStatistics = new DescriptiveStatistics();

        mReadPathNetStatistics = new DescriptiveStatistics();
        mWriteBackNetStatistics = new DescriptiveStatistics();

        mAddPathStatistics = new DescriptiveStatistics();

        mReadPathStartTimes = new ConcurrentHashMap<>();
        mWriteBackStartTimes = new ConcurrentHashMap<>();

        mReadPathPreSendTimes = new ConcurrentHashMap<>();
        mWriteBackPreSendTimes = new ConcurrentHashMap<>();

        mReadPathSendToRecvTimes = new ConcurrentHashMap<>();
        mWriteBackSendToRecvTimes = new ConcurrentHashMap<>();
    }

    public void writeStatistics() {
        String report = null;
        String filename = null;

        filename = "readPathStats"+unitID+".txt";
        synchronized (mReadPathStatistics) {
            report = mReadPathStatistics.toString();
        }
        // Write the report to a file
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println(report);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        filename = "writeBackStats"+unitID+".txt";
        synchronized (mWriteBackStatistics) {
            report = mWriteBackStatistics.toString();
        }
        // Write the report to a file
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println(report);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        filename = "readPathSendToRecvStats"+unitID+".txt";
        synchronized (mReadPathSendToRecvStatistics) {
            report = mReadPathSendToRecvStatistics.toString();
        }
        // Write the report to a file
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println(report);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        filename = "writeBackSendToRecvStats"+unitID+".txt";
        synchronized (mWriteBackSendToRecvStatistics) {
            report = mWriteBackSendToRecvStatistics.toString();
        }
        // Write the report to a file
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println(report);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        filename = "readPathServerProcessingStats"+unitID+".txt";
        synchronized (mReadPathServerStatistics) {
            report = mReadPathServerStatistics.toString();
        }
        // Write the report to a file
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println(report);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        filename = "writeBackServerProcessingStats"+unitID+".txt";
        synchronized (mWriteBackServerStatistics) {
            report = mWriteBackServerStatistics.toString();
        }
        // Write the report to a file
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println(report);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        filename = "readPathNetStats"+unitID+".txt";
        synchronized (mReadPathNetStatistics) {
            report = mReadPathNetStatistics.toString();
        }
        // Write the report to a file
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println(report);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        filename = "writeBackNetStats"+unitID+".txt";
        synchronized (mWriteBackNetStatistics) {
            report = mWriteBackNetStatistics.toString();
        }
        // Write the report to a file
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println(report);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        filename = "addPathStats"+unitID+".txt";
        synchronized (mAddPathStatistics) {
            report = mAddPathStatistics.toString();
        }
        // Write the report to a file
        try {
            PrintWriter writer = new PrintWriter(filename);
            writer.println(report);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void readPathStart(ClientRequest req) {
        mReadPathStartTimes.put(req, System.currentTimeMillis());
    }

    public void readPathComplete(ClientRequest req) {

        if (mReadPathStartTimes.containsKey(req)) {
            long readPathStartTime = mReadPathStartTimes.get(req);
            mReadPathStartTimes.remove(req);
            System.out.println("removed req"+req.getRequestID());

            long totalTime = System.currentTimeMillis() - readPathStartTime;
            synchronized (mReadPathStatistics) {
                mReadPathStatistics.addValue(totalTime);
            }
        }
        //TaoLogger.logForce("read path time (ms): " + (totalTime));
    }

    public void writeBackStart(long writeBackTime) {
        //TaoLogger.logForce("writeBackStart");
        mWriteBackStartTimes.put(writeBackTime, System.currentTimeMillis());
    }

    public void writeBackComplete(long writeBackTime) {
        //TaoLogger.logForce("writeBackComplete");

        // Profiling
        if (mWriteBackStartTimes.containsKey(writeBackTime)) {
            long writeBackStartTime = mWriteBackStartTimes.get(writeBackTime);
            mWriteBackStartTimes.remove(writeBackTime);

            long totalTime = System.currentTimeMillis() - writeBackStartTime;
            synchronized (mWriteBackStatistics) {
                mWriteBackStatistics.addValue(totalTime);
            }
        }
        //TaoLogger.logForce("write back time (ms): " + (totalTime));
    }

    public void readPathServerProcessingTime(InetSocketAddress address, ClientRequest req, long processingTime) {
        Map<ClientRequest, Long> readPathSendToRecvTimesForServer = mReadPathSendToRecvTimes.get(address);
        long t2 = readPathSendToRecvTimesForServer.get(req);
        long netTimeApprox = t2 - processingTime;

        //TaoLogger.logForce("readPathNet time (" + address + ", " + req.getRequestID() + "): " + netTimeApprox);

        synchronized (mReadPathServerStatistics) {
            mReadPathServerStatistics.addValue(processingTime);
        }

        synchronized (mReadPathNetStatistics) {
            mReadPathNetStatistics.addValue(netTimeApprox);
        }
    }

    public void writeBackServerProcessingTime(InetSocketAddress address, long writeBackTime, long processingTime) {
        Map<Long, Long> writeBackSendToRecvTimesForServer = mWriteBackSendToRecvTimes.get(address);
        long t2 = writeBackSendToRecvTimesForServer.get(writeBackTime);
        long netTimeApprox = t2 - processingTime;

        synchronized (mWriteBackServerStatistics) {
            mWriteBackServerStatistics.addValue(processingTime);
        }

        synchronized (mWriteBackNetStatistics) {
            mWriteBackNetStatistics.addValue(netTimeApprox);
        }
    }

    public void readPathPreSend(InetSocketAddress address, ClientRequest req) {
        if (!mReadPathPreSendTimes.containsKey(address)) {
            mReadPathPreSendTimes.put(address, new ConcurrentHashMap<>());
        }

        Map<ClientRequest, Long> serverReadPathPreSendTimes = mReadPathPreSendTimes.get(address);
        serverReadPathPreSendTimes.put(req, System.currentTimeMillis());
    }

    public void readPathPostRecv(InetSocketAddress address, ClientRequest req) {
        long t2 = System.currentTimeMillis();
        if (!mReadPathSendToRecvTimes.containsKey(address)) {
            mReadPathSendToRecvTimes.put(address, new ConcurrentHashMap<>());
        }

        System.out.println("readPathPostRecv "+req.getRequestID());

        Map<ClientRequest, Long> readPathSendToRecvTimesForServer = mReadPathSendToRecvTimes.get(address);
        long t1 = mReadPathPreSendTimes.get(address).get(req);

        synchronized (mReadPathSendToRecvStatistics) {
            mReadPathSendToRecvStatistics.addValue(t2 - t1);
        }
        //TaoLogger.logForce("readPathSendToRecv time (" + address + ", " + req.getRequestID() + "): " + (t2-t1));
        readPathSendToRecvTimesForServer.put(req, t2 - t1);
    }

    public void writeBackPreSend(InetSocketAddress address, long writeBackTime) {
        if (!mWriteBackPreSendTimes.containsKey(address)) {
            mWriteBackPreSendTimes.put(address, new ConcurrentHashMap<>());
        }

        Map<Long, Long> serverWriteBackPreSendTimes = mWriteBackPreSendTimes.get(address);
        serverWriteBackPreSendTimes.put(writeBackTime, System.currentTimeMillis());
    }

    public void writeBackPostRecv(InetSocketAddress address, long writeBackTime) {
        long t2 = System.currentTimeMillis();
        if (!mWriteBackSendToRecvTimes.containsKey(address)) {
            mWriteBackSendToRecvTimes.put(address, new ConcurrentHashMap<>());
        }

        Map<Long, Long> writeBackSendToRecvTimesForServer = mWriteBackSendToRecvTimes.get(address);
        long t1 = mWriteBackPreSendTimes.get(address).get(writeBackTime);

        synchronized (mWriteBackSendToRecvStatistics) {
            mWriteBackSendToRecvStatistics.addValue(t2 - t1);
        }
        writeBackSendToRecvTimesForServer.put(writeBackTime, t2 - t1);
    }

    public void addPathTime(long processingTime) {
        synchronized (mAddPathStatistics) {
            mAddPathStatistics.addValue(processingTime);
        }
    }

}
