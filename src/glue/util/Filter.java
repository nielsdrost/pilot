package glue.util;

import java.io.FilenameFilter;

/**
 * A simple Filter that selects all files that have a specific filename extension.
 * 
 * @author jason
 */
public class Filter implements FilenameFilter {

    final String ext;

    public Filter(String ext) {
        this.ext = ext;
    }

    @Override
    public boolean accept(java.io.File dir, String name) {
        return name.endsWith(ext); 
    }
}
