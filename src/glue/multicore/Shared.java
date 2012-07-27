package glue.multicore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


import glue.util.Filter;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.PortType;

/** 
 * Class containing various utility functions and constants.
 * 
 * @author jason
 */
public class Shared {

    /** PortType used in communication to PilotJobs */
    public static final PortType portTypeSlave = new PortType(
            PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
            PortType.RECEIVE_EXPLICIT, PortType.CONNECTION_ONE_TO_ONE);

    /** PortType used in communication to JobServer */
    public static final PortType portTypeServer = new PortType(
            PortType.COMMUNICATION_RELIABLE, PortType.SERIALIZATION_OBJECT,
            PortType.RECEIVE_AUTO_UPCALLS, PortType.CONNECTION_MANY_TO_ONE);

    /** IbisCapabilities needed for both JobsServer and PilotJobs */
    public static final IbisCapabilities ibisCapabilities = new IbisCapabilities(
            IbisCapabilities.ELECTIONS_STRICT, IbisCapabilities.TERMINATION);

    /** Read a file input a byte array */
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

    /** Write a byte array to a file. Also created directory if needed */
    public static void write(String fileName, byte[] data) throws IOException {
        File file = new File(fileName);
        file.getAbsoluteFile().getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(file);
        out.write(data);
        out.close();
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
}
