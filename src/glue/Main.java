package glue;

import ibis.ipl.server.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

import org.gridlab.gat.GAT;
import org.gridlab.gat.GATObjectCreationException;
import org.gridlab.gat.URI;
import org.gridlab.gat.resources.JavaSoftwareDescription;
import org.gridlab.gat.resources.Job;
import org.gridlab.gat.resources.Job.JobState;
import org.gridlab.gat.resources.JobDescription;
import org.gridlab.gat.resources.ResourceBroker;

/** 
 * The main class that runs the JobServer and deploys the PilotJobs.
 * 
 * @author jason
 */
public class Main {

    /** A tuple to store (brokerURI, javaLocation) pairs) */
    static class Resource {
        String brokerURI;
        String javaLocation;

        Resource(String brokerURI, String javaLocation) {
            this.brokerURI = brokerURI;
            this.javaLocation = javaLocation;
        }
    }

    /** A Job counter */
    static int number = 0;

    /**
     * Creates a JobDescription for a PilotJob 
     * 
     * @param serverAddress the location of the Ibis Server. 
     * @param javaLocation path to the java executable on the target resource.
     * @return a JodDescription for starting a PilotJob 
     * @throws GATObjectCreationException if creating the JobDescription failed.  
     */
    public static JobDescription prepareJob(String serverAddress,
            String javaLocation) throws GATObjectCreationException {

        JavaSoftwareDescription sd = new JavaSoftwareDescription();

        // Contact information properties needed to find the IPL server 
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("ibis.server.address", serverAddress);
        properties.put("ibis.pool.name", "PILOT");

        // Set the executable (java), the classpath (jars), the system 
        // properties (IPL server location), and the main class.   
        sd.setExecutable(javaLocation);
        sd.setJavaClassPath("ipl/*:glue-examples.jar:.");
        sd.setJavaSystemProperties(properties);
        sd.setJavaMain("glue.PilotJob");

        // Create files for stdout and stderr
        sd.setStdout(GAT.createFile("stdout-" + number + ".txt"));
        sd.setStderr(GAT.createFile("stderr-" + number + ".txt"));

        // Prestage the libraries and property files.
        sd.addPreStagedFile(GAT.createFile("lib/glue-examples.jar"));
        sd.addPreStagedFile(GAT.createFile("deploy/lib-server"), GAT.createFile("ipl"));
        sd.addPreStagedFile(GAT.createFile("log4j.properties"));

        number++;

        return new JobDescription(sd);
    }

    /**
     * Waits until all jobs have terminated.
     * 
     * @param jobs the Jobs to wait for.
     */
    public static void waitUntilDone(LinkedList<Job> jobs) {
        for (Job job : jobs) {
            while ((job.getState() != JobState.STOPPED) && (job.getState() != JobState.SUBMISSION_ERROR)) {
                try { 
                    Thread.sleep(500);
                } catch (Exception e) {
                    // ignored -- will not occur.
                }
            }
        }
    }

    /** Main method that parses the parameters and starts the Main class */
    public static void main(String[] args) throws Exception {

        LinkedList<Resource> resources = new LinkedList<Resource>();

        String[] arguments = null;
        String executable = null;
        String inputdir = null;
        String outputdir = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--resource")) {
                resources.add(new Resource(args[++i], args[++i]));

            } else if (args[i].startsWith("--executable")) {
                executable = args[++i];

            } else if (args[i].startsWith("--arguments")) {
                ArrayList<String> tmp = new ArrayList<String>();
                while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    tmp.add(args[++i]);
                }
                arguments = tmp.toArray(new String[tmp.size()]);

            } else if (args[i].startsWith("--input")) {
                inputdir = args[++i];

            } else if (args[i].startsWith("--output")) {
                outputdir = args[++i];

            } else {
                System.err.println("Unknown option: " + args[i]);
                System.exit(1);
            }
        }

        if (resources.size() == 0 || arguments == null || executable == null
                || inputdir == null || outputdir == null) {
            System.err.println("Missing arguments!");
            System.exit(1);
        }

        Server reg = new Server(new Properties());

        Properties p = new Properties();
        p.put("ibis.server.address", reg.getAddress());
        p.put("ibis.pool.name", "PILOT");

        JobServer jobServer = new JobServer(executable, arguments, inputdir,
                outputdir, p);

        LinkedList<Job> gatJobs = new LinkedList<Job>();

        for (Resource resource : resources) {
            System.out.println("Deploying to resource: " + resource.brokerURI);
        
            ResourceBroker broker = GAT.createResourceBroker(new URI(resource.brokerURI));
            Job gatJob = broker.submitJob(prepareJob(reg.getAddress(), resource.javaLocation));            
            gatJobs.add(gatJob);
        }

        System.out.println("Starting server");
        
        jobServer.run();

        System.out.println("Cleaning up");
        
        waitUntilDone(gatJobs);
        
        GAT.end();
        
        System.out.println("Done!");
    }
}
