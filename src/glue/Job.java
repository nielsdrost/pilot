package glue;

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

    /** The command to execute */
    String[] command;
    
    /** The name of the input file */
    String inputFile;
    
    /** The name of the output file */
    String outputFile;
    
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
    Job(String executable, String[] arguments, String input, String output) {
        empty = false;

        this.inputFile = input;
        this.outputFile = output;

        command = new String[arguments.length + 3];
        command[0] = executable;
        command[1] = input;
        System.arraycopy(arguments, 0, command, 2, arguments.length);
        command[command.length - 1] = output;
    }

    /**
     * Set the input data array of the job. 
     *
     * @param inputBuffer the input data
     */
    public void setInput(byte[] inputBuffer) {
        this.inputBuffer = inputBuffer;
    }

    /** Execute the job and return the result */
    public Result execute() {

        try {
            long start = System.currentTimeMillis();

            System.out.println("Executing Job: " + Arrays.toString(command));

            // Store the input data on disk and release the memory buffer.
            Shared.write(inputFile, inputBuffer);
            inputBuffer = null;

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
            }

            long time = System.currentTimeMillis() - start;

            return new Result(outputFile, outputBuffer, stdout, stderr, status, time);
        } catch (Exception e) {
            return new Result(e);
        }
    }
}
