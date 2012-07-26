package glue;

import java.io.Serializable;

/** 
 * The Result of a Job.  
 *  
 * @author jason
 */
public class Result implements Serializable {

    private static final long serialVersionUID = 7331001105371799850L;

    /** Exit code of the Job */
    int status;
    
    /** Stdout of the Job */
    byte[] stdout;
    
    /** Stderr of the Job */
    byte[] stderr;

    /** Exception thrown during the execution of the Job. */
    Exception e;
    
    /** Time needed to execute the Job. */
    long time;

    /** Name of the output file */
    String outputFile;

    /** Buffer containing the output file */
    byte[] outputBuffer;
        
    /** Flag to indicate that this job is empty */
    final boolean empty;

    /** Constructor to create an empty Result */
    Result() {
        empty = true;
    }

    /** Constructor to create an exception-only result */
    Result(Exception e) {
        empty = false;
        this.e = e;
        status = 1;
    }

    /** Constructor to create a regular result */
    Result(String outputFile, byte[] outputBuffer, byte[] stdout,
            byte[] stderr, int status, long time) {
        this.empty = false;
        this.outputFile = outputFile;
        this.outputBuffer = outputBuffer;
        this.stdout = stdout;
        this.stderr = stderr;
        this.status = status;
        this.time = time;
    }
}
