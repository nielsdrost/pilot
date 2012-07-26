package glue.multicore;

import ibis.util.RunProcess;

import java.io.Serializable;
import java.util.Arrays;

/**
 * This object represents a Job. It consists of a command array containing the command to
 * execute (typically a executable plus its parameters), a byte array containing the input 
 * data, and the names of the input and output files.   
 * 
 * @author jason
 */
public class Job implements Serializable {

    private static final long serialVersionUID = 4957475242030816948L;

    /** The job ID */
    int jobID;
    
    /** The script to execute */
    String script;
    
    /** The name of the input file */
    String inputFile;
    
    /** The name of the output file */
    String outputFile;

    /** The script to run */
    byte[] scriptBuffer;
    
    /** The input data */
    byte[] inputBuffer;

    /** A flag to indicate that this job is empty */
    boolean empty;
    
    /** Constructor to create an empty job */
    Job() {
        empty = true;
    }

    /**
     * Constructor to create a regular job. 
     *      
     * @param executable path to the executable (assumed to be available on target resource).
     * @param arguments command line arguments for executable
     * @param input path to input file (on local machine) 
     * @param output path to output file (on local machine)
     */
    Job(int ID, String script, String input, String output) {
        empty = false;
        
        this.jobID = ID;
        this.script = script;
        this.inputFile = input;
        this.outputFile = output;
    }

    /**
     * Set the input data array of the job. 
     *
     * @param inputBuffer the input data
     */
    public void setData(byte [] scriptBuffer, byte[] inputBuffer) {
        this.scriptBuffer = scriptBuffer;
        this.inputBuffer = inputBuffer;
    }

    /** Execute the job and return the result */
    public Result execute() {

        try {
            long start = System.currentTimeMillis();

            // Generate a unique script name
            String scriptName = "script-" + jobID + ".sh";

            // Store the script and input data on disk and release the memory buffer.
            Shared.write(scriptName, scriptBuffer);
            Shared.write(inputFile, inputBuffer);
            scriptBuffer = null;
            inputBuffer = null;
            
            // Generate the command to execute.
            String [] command = new String [] { "/bin/sh", scriptName, inputFile, outputFile };

            System.out.println("Executing Job: " + Arrays.toString(command));
            
            // Execute the command.
            RunProcess p = new RunProcess(command);
            p.run();

            // Extract the exit code, stdout, and stderr. 
            int status = p.getExitStatus();
            byte[] stderr = p.getStderr();
            byte[] stdout = p.getStdout();
            byte[] outputBuffer = null;

            // Read the output file.
            if (status == 0) {
                outputBuffer = Shared.read(outputFile);
            } else {
                System.err.println("Error on running job");
                System.err.write(stdout);
                System.err.write(stderr);
            }

            long time = System.currentTimeMillis() - start;

            return new Result(outputFile, outputBuffer, stdout, stderr, status, time);
        } catch (Exception e) {
            System.err.println("Error on running job");
            e.printStackTrace(System.err);
            return new Result(e);
        }
    }
}
