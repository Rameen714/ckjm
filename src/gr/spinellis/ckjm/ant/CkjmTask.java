/*
 * (C) Copyright 2005 Diomidis Spinellis, Julien Rentrop
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package gr.spinellis.ckjm.ant;

import gr.spinellis.ckjm.CkjmOutputHandler;
import gr.spinellis.ckjm.MetricsFilter;
import gr.spinellis.ckjm.PrintPlainResults;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Path;

/**
 * Ant task definition for the CKJM metrics tool.
 *
 * @version $Revision: 1.3 $
 * @author Julien Rentrop
 */
public class CkjmTask extends MatchingTask {
    private File outputFile;

    private File classDir;

    private Path extdirs;

    private String format;

    public CkjmTask() {
        this.format = "plain";
    }

    /**
     * Sets the format of the output file.
     *
     * @param format
     *            the format of the output file. Allowable values are 'plain' or
     *            'xml'.
     */
    public void setFormat(String format) {
        this.format = format;

    }

    /**
     * Sets the outputfile
     *
     * @param outputfile
     *            Location of outputfile
     */
    public void setOutputfile(File outputfile) {
        this.outputFile = outputfile;
    }

    /**
     * Sets the dir which contains the class files that will be analyzed
     *
     * @param classDir
     *            Location of class files
     */
    public void setClassdir(File classDir) {
        this.classDir = classDir;
    }

    /**
     * Sets the extension directories that will be used by ckjm.
     * @param e extdirs a path containing .jar files
     */
    public void setExtdirs(Path e) {
        if (extdirs == null) {
            extdirs = e;
        } else {
            extdirs.append(e);
        }
    }

    /**
     * Gets the extension directories that will be used by ckjm.
     * @return the extension directories as a path
     */
    public Path getExtdirs() {
        return extdirs;
    }

    /**
     * Adds a path to extdirs.
     * @return a path to be modified
     */
    public Path createExtdirs() {
        if (extdirs == null) {
            extdirs = new Path(getProject());
        }
        return extdirs.createPath();
    }
    
    private void validateParameters() {
        if (classDir == null) {
            throw new BuildException("classDir attribute must be set!");
        }
        if (!classDir.exists()) {
            throw new BuildException("classDir does not exist!");
        }
        if (!classDir.isDirectory()) {
            throw new BuildException("classDir is not a directory!");
        }
    }

    private void setJavaExtensionDirectories() {
    if (extdirs != null && extdirs.size() > 0) {
        String extDirsProperty = System.getProperty("java.ext.dirs");
        if (extDirsProperty == null || extDirsProperty.isEmpty()) {
            System.setProperty("java.ext.dirs", extdirs.toString());
        } else {
            System.setProperty("java.ext.dirs",
                extDirsProperty + File.pathSeparator + extdirs.toString());
        }
    }
}
    
    private String[] buildFilePaths(String[] files) {
        String[] filePaths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            filePaths[i] = classDir.getPath() + File.separatorChar + files[i];
        }
        return filePaths;
    }

    private CkjmOutputHandler createOutputPrinter(OutputStream outputStream) {
        if (format.equals("xml")) {
            return new PrintXmlResults(new PrintStream(outputStream));
        } else {
            return new PrintPlainResults(new PrintStream(outputStream));
        }
    }
    
    /**
     * Executes the CKJM Ant Task. This method redirects the output of the CKJM
     * tool to a file. When XML format is used it will buffer the output and
     * translate it to the XML format.
     *
     * @throws BuildException
     *             if an error occurs.
     */
    public void execute() throws BuildException {
        
        validateParameters();
        
	setJavaExtensionDirectories();

        DirectoryScanner ds = super.getDirectoryScanner(classDir);

        String files[] = ds.getIncludedFiles();
        
        if (files.length == 0) {
            log("No class files in specified directory " + classDir);
        } 
        else {
            String[] filePaths = buildFilePaths(files);
            try {
                OutputStream outputStream = new FileOutputStream(outputFile);                
                CkjmOutputHandler outputPrinter = createOutputPrinter(outputStream);
                MetricsFilter.runMetrics(filePaths, outputPrinter);

            } catch (IOException ioe) {
                throw new BuildException("Error file handling: "
                        + ioe.getMessage());
            }
        }
    }
}