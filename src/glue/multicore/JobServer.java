package glue.multicore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import glue.util.FileUtils;
import ibis.ipl.Ibis;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

/**
 * A JobServer that hands out Job objects to running PilotJobs.
 * 
 * @author jason
 */
public class JobServer implements MessageUpcall {

    private final Logger logger = LoggerFactory.getLogger(JobServer.class);

    private static class ShutDownHook extends Thread {

        private final JobServer jobServer;

        public ShutDownHook(JobServer jobServer) {
            this.jobServer = jobServer;
        }

        public void run() {
            jobServer.stop();
        }

    }

    private final HashSet<String> jobsRead = new HashSet<String>();

    private final String script;

    /** The input directory */
    String inputDir;

    /** The output directory */
    String outputDir;

    /** A list of all jobs */
    BlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();

    /** The Ibis to use */
    Ibis ibis;

    /** The ReceivePort for incoming job requests and results. */
    ReceivePort rp;

    /**
     * A HashMap containing the IDs of all known workers and the SendPorts to
     * reach them
     */
    HashMap<IbisIdentifier, SendPort> workers = new HashMap<IbisIdentifier, SendPort>();

    /** A job counter */
    int number = 0;

    private boolean terminated = false;

    /**
     * Constructor to create a JobServer
     * 
     * @param executable
     *            path to the executable (assumed to be available on remote
     *            machine).
     * @param arguments
     *            command line arguments for the executable.
     * @param inputDir
     *            path to a local input directory.
     * @param outputDir
     *            path to a local output directory.
     * @param p
     *            properties needed to initialize the IPL.
     * @throws Exception
     *             if the IPL failed to initialize.
     */
    JobServer(String script, String inputDir, String outputDir, Properties p) throws Exception {

        this.script = script;

        this.inputDir = inputDir;
        this.outputDir = outputDir;

        ibis = IbisFactory.createIbis(Shared.ibisCapabilities, p, true, null, Shared.portTypeServer,
                Shared.portTypeSlave);

        ibis.registry().elect("JobServer");

        rp = ibis.createReceivePort(Shared.portTypeServer, "receiver", this);
        rp.enableConnections();
    }

    private synchronized boolean hasTerminated() {
        return terminated;
    }

    private synchronized void setTerminated() {
        terminated = true;
    }

    public void stop() {
        try {
            ibis.registry().terminate();
        } catch (IOException e) {
            System.err.println("Failed to terminate");
            e.printStackTrace(System.err);
        }
        setTerminated();
    }

    /**
     * Retrieves a SendPort for a PilotJob.
     * 
     * If necessary, a new SendPort is created and connected to the target
     * PilotJob. This SendPort will be cached for later use.
     * 
     * @param target
     *            the IbisIdentifier of the target PilotJob.
     * @return a SendPort connected to the target PilotJob.
     * @throws IOException
     *             if the connection setup failed.
     */
    SendPort getSendPort(IbisIdentifier target) throws IOException {
        SendPort sp = workers.get(target);

        if (sp == null) {
            sp = ibis.createSendPort(Shared.portTypeSlave);
            sp.connect(target, "receiver");
            workers.put(target, sp);
        }

        return sp;
    }

    /**
     * Closes and removes the SendPort for a PilotJob.
     * 
     * @param target
     *            the target PilotJob.
     */
    void removeSendPort(IbisIdentifier target) {
        SendPort sp = workers.remove(target);

        if (sp != null) {
            try {
                sp.close();
            } catch (Exception e) {
                System.err.println("Failed to close Sendport to " + target + ": " + e);
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Send a job to a PilotJob
     * 
     * @param target
     *            the target PilotJob.
     * @param job
     *            the Job to send.
     * @throws IOException
     *             if the send has failed.
     */
    void sendReply(IbisIdentifier target, Job job) throws IOException {
        SendPort sp = getSendPort(target);

        if (!job.empty) {
            job.setData(Shared.read(job.script), Shared.read(inputDir + File.separator + job.inputFile));
        }

        WriteMessage wm = sp.newMessage();
        wm.writeObject(job);
        wm.finish();

        if (job.empty) {
            removeSendPort(target);
        }
    }

    /**
     * Store an output file in the local output directory.
     * 
     * @param result
     *            the Result to process.
     */
    void processResult(Result result) {
        if (!result.empty && result.status == 0) {
            try {
                Shared.write(outputDir + File.separator + result.outputFile, result.outputBuffer);
            } catch (Exception e) {
                System.out.println("Failed to store output file: " + result.outputFile);
                e.printStackTrace();
            }
        }
    }

    @Override
    /** Handle an incoming message */
    public void upcall(ReadMessage rm) throws IOException, ClassNotFoundException {
        IbisIdentifier target = rm.origin().ibisIdentifier();
        Result result = (Result) rm.readObject();
        
        //will trigger new upcall for next message
        rm.finish();
        
        processResult(result);

        Job job;
        try {
            job = jobs.take();
            System.out.println("Sending job " + job.jobID + " to " + target);
        } catch (InterruptedException e) {
            job = new Job();
            System.out.println("Sending empty job to " + target);
        }

        sendReply(target, job);
    }

    private void readJobs() {

        logger.debug("Reading jobs...");

        for (String file : FileUtils.listFiles(inputDir)) {
            if (!jobsRead.contains(file)) {
                jobs.add(new Job(number++, script, file, "out-" + file));
                
                jobsRead.add(file);
                logger.debug("Added new job " + file);
            } else {
                logger.debug("Not adding job " + file + ", already seen");
            }
        }
    }

   

    /** Main loop */
    void run() throws Exception {
        rp.enableMessageUpcalls();
        while (!hasTerminated()) {
            readJobs();

            Thread.sleep(1000);
        }
    }

    /** Main method for stand-alone job server (for use in IbisDeploy). */
    public static void main(String[] args) throws Exception {
        String script = null;
        String inputdir = null;
        String outputdir = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--script")) {
                script = args[++i];

            } else if (args[i].startsWith("--input")) {
                inputdir = args[++i];

            } else if (args[i].startsWith("--output")) {
                outputdir = args[++i];

            } else {
                System.err.println("Unknown option: " + args[i]);
                System.exit(1);
            }
        }

        if (script == null || inputdir == null || outputdir == null) {
            System.err.println("Missing arguments!");
            System.exit(1);
        }

        JobServer jobServer = new JobServer(script, inputdir, outputdir, new Properties());

        Runtime.getRuntime().addShutdownHook(new ShutDownHook(jobServer));

        jobServer.run();
    }

   
}
