package glue.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {

    /** 
     * Read a file into a byte array. 
     * 
     * @param file path to the file to read.
     * @return a byte array containing the content of the file.
     * @throws IOException if the file could not be read.
     */
    public static byte[] read(String file) throws IOException {

        File tmp = new File(file);

        if (tmp.exists() && tmp.canRead() && tmp.isFile()) {

            byte[] buffer = new byte[(int) tmp.length()];

            FileInputStream f = new FileInputStream(tmp);

            int offset = 0;
            int read = 0;

            while (offset < buffer.length) {
                read = f.read(buffer, offset, buffer.length - offset);

                if (read == -1) {
                    throw new FileNotFoundException("Cannot read " + file);
                }

                offset += read;
            }

            return buffer;
        }

        throw new FileNotFoundException("Cannot access " + file);
    }

     
    /** Write a byte array to a file. Also created directory if needed. Writes via a tmp file, then moves to the final file */
    public static void write(String fileName, byte[] data) throws IOException {
        File file = new File(fileName);
        file.getAbsoluteFile().getParentFile().mkdirs();
        File tmpFile = new File(fileName + ".tmp");
        FileOutputStream out = new FileOutputStream(tmpFile);
        out.write(data);
        out.close();
        tmpFile.renameTo(file);
    }

    /** List all files in a directory with a specific extention. */
    public static String[] listFiles(String directory, String extention) {

        File inputdir = new File(directory);

        if (!inputdir.isDirectory()) {
            System.err.println("Need directory as input!");
            System.exit(1);
        }

        return inputdir.list(new Filter(extention));
    }
    
    /** List all files in a directory with a specific extention. */
    public static String[] listFiles(String directory) {

        File inputdir = new File(directory);

        if (!inputdir.isDirectory()) {
            System.err.println("Need directory as input!");
            System.exit(1);
        }

        return inputdir.list();
    }
    
    /** 
     * Close a Closeable (such as a stream or file) while ignoring any exceptions.
     *  
     * @param c the Closable to close.
     */    
    public static void close(Closeable c) {
        try { 
            if (c != null) { 
                c.close();
            }
        } catch (Exception e) {
            // ignored
        }
    }

    public static boolean ensureFileExists(String file) {
        File tmp = new File(file);
        return (tmp.exists() && tmp.isFile() && tmp.canRead());
    }

    public static boolean ensureDirExists(String dir) {
        File tmp = new File(dir);
        return (tmp.exists() && tmp.isDirectory() && tmp.canRead());
    }    
}
