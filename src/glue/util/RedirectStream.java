package glue.util;

import java.io.InputStream;
import java.io.OutputStream;

public class RedirectStream extends Thread {
    
    private InputStream source;
    private OutputStream destination;
    
    private boolean done = false;
    
    public RedirectStream(InputStream source, OutputStream destination) {
        this.source = source;
        this.destination = destination;
        setDaemon(true);
    }

    public void run() { 
        byte [] buffer = new byte[1024];        
        int bytes = 0;
        
        do { 
            try { 
                bytes = source.read(buffer);
            
                if (bytes > 0) {
                    destination.write(buffer, 0, bytes);
                }
            } catch (Exception e) {
                bytes = -1;
            }
        } while (bytes > 0);
        
        synchronized (this) {
            done = true;
            notifyAll();
        }        
    }
    
    public synchronized void waitUntilDone() { 
        while (!done) { 
            try {
                wait();
            } catch (InterruptedException e) {
                // ignored
            }
        }
    }
}
