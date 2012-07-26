package glue.multicore;

import ibis.ipl.Ibis;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

/** PilotJob which retrieves jobs from the JobServer and executes them. */
public class PilotJob extends Thread {

    /** The ibis used for communication */
    Ibis ibis;
    
    /** The receiveport used to receive jobs */
    ReceivePort rp;
    
    /** The sendport used to return results and job requests */
    SendPort sp;

    /** 
     * Constructor that creates a PilotJob 
     * 
     * @throws Exception if Ibis failed to initialize. 
     */
    PilotJob() throws Exception {
        // Create an Ibis
        ibis = IbisFactory.createIbis(Shared.ibisCapabilities, null,
                Shared.portTypeServer, Shared.portTypeSlave);

        // Retrieve the identifier of the JobServer
        IbisIdentifier server = ibis.registry().getElectionResult("JobServer");

        // Create a receiveport and enable connections 
        rp = ibis.createReceivePort(Shared.portTypeSlave, "receiver");
        rp.enableConnections();

        // Create a sendport and connect it to the JobServer 
        sp = ibis.createSendPort(Shared.portTypeServer);
        sp.connect(server, "receiver");
    }

    /** 
     * Send a result to the JobServer and receive a new Job.
     * 
     * @param previousResult the result to send to the JobServer. 
     * @return the new Job returned by the JobServer.
     * @throws Exception if the communication failed. 
     */
    Job getWork(Result previousResult) throws Exception {
        WriteMessage wm = sp.newMessage();
        wm.writeObject(previousResult);
        wm.finish();

        ReadMessage rm = rp.receive();
        Job job = (Job) rm.readObject();
        rm.finish();
        return job;
    }

    /** Main loop */
    public void run() {
        try { 
            Job job = getWork(new Result());

            while (!job.empty) {
                Result result = job.execute();
                job = getWork(result);
            }
    
            ibis.end();
        
        } catch (Exception e) {
            System.err.println("PilotJob failed: " + e);
            e.printStackTrace(System.err);
        }
    }

    /** Main method that creates and starts the PilotJob */
    public static void main(String[] args) {
        
        try {
            // Detect the number of cores on this machine
            int cores = Runtime.getRuntime().availableProcessors();
            
            // Create and start one thread for each core.
            PilotJob [] pilots = new PilotJob[cores];
            
            for (int i=0;i<cores;i++) { 
                pilots[i] = new PilotJob();
                pilots[i].start();
            }

            // Wait until all threads have terminated.
            for (int i=0;i<cores;i++) { 
                pilots[i].join();
            }
 
        } catch (Exception e) {
            System.err.println("PilotJob failed: " + e);
            e.printStackTrace(System.err);
        }
    }
}

