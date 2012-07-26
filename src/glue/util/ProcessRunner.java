package glue.util;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.util.Arrays;

public class ProcessRunner extends Thread {
    
    private ProcessBuilder builder;    
    private Process process;
    
    private String stdout;        
    private String stderr;

    private FileOutputStream out = null; 
    private FileOutputStream err = null; 
    
    private RedirectStream redirectOut;
    private RedirectStream redirectErr;
    
    private ProcessRunnerCallBack callback;
    private int ID;
    private Object info;
    
    private int exitValue;    
    
    private long startTime; 
    
    public ProcessRunner(int ID, Object info, String [] command, String stdout, String stderr, ProcessRunnerCallBack callback) {
        this.ID = ID;
        this.info = info;
        this.stdout = stdout;
        this.stderr = stderr;
        this.callback = callback;

        builder = new ProcessBuilder(command);
        
        System.out.println("Creating process runner for " + Arrays.toString(command));
    }
    
    private void close(Closeable c) { 
        if (c != null) {
            try { 
                c.close();
            } catch (Exception e) {
                // ignored
            }
        }
    }
    
    private void cleanup() { 
        redirectOut.waitUntilDone();
        redirectErr.waitUntilDone();
        
        close(out);
        close(err);

        if (process != null) { 
            close(process.getOutputStream());
            process.destroy();
            process = null;
        }
        
        long endTime = System.currentTimeMillis();
        
        callback.processTerminated(ID, info, exitValue, (endTime-startTime), "done");           
    }
    
    public void run() {
        
        try { 
            out = new FileOutputStream(stdout);
            err = new FileOutputStream(stderr);
            
            startTime = System.currentTimeMillis();
            
            synchronized (this) { 
                process = builder.start();
            } 
        } catch (Exception e) {
            callback.processTerminated(ID, info, -1, 0, "Failed to start process: " + e);
            e.printStackTrace();
            
            close(out);
            close(err);
            return;    
        } 

        redirectOut = new RedirectStream(process.getInputStream(), out);
        redirectOut.start();
        
        redirectErr = new RedirectStream(process.getErrorStream(), err);
        redirectErr.start();           

        try { 
            exitValue = process.waitFor();
        } catch (InterruptedException e) {
            // ignored, does not occur.
        }
        
        cleanup();            
    }
    
    public synchronized void kill() { 
        if (process != null) { 
            process.destroy();
        }
    }
}
