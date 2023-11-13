package com.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Named pipe that uses a FIFO on the Linux platform.
 * Also works on Solaris and many other Unices without change.
 */
class LinuxNamedPipe extends NamedPipe {

  private static final String PIPE_MODE = "644";
  private static final String CHMOD_CMD = "chmod";
  private static final String MKFIFO_CMD = "mkfifo";
  
  private FileInputStream inputStream;
  private OutputStream outputStream;
  //public static Logger log = Logger.getLogger(LinuxNamedPipe.class);
 
  final static String getNativeName(String name) {
    return String.format("/tmp/%s", name);
  }

  LinuxNamedPipe(String name, boolean createFIFO) throws IOException {
    super(getNativeName(name));
    String pipeName = getPipeName();
    if (createFIFO) {
      if (!(new File(pipeName)).exists()) {
        // create the FIFO in the filesystem
        ProcessUtil.runCommand(new String[] {MKFIFO_CMD, pipeName});
        // change mode to allow mysqld process to read the pipe
        ProcessUtil.runCommand(new String[] {CHMOD_CMD, PIPE_MODE, pipeName});
        //log.debug(String.format("created named pipe %s for server", pipeName));
      } else {
        throw new RuntimeException("Can't create named pipe \"" + pipeName +
        "\" as it already exists.");
      }
    }
  }
  
  @Override
  public void connect() throws IOException {
  }
  
  @Override
  public int read(byte[] bytes, int offset, int length) throws IOException {
    if (inputStream == null) {
      inputStream = new FileInputStream(getPipeName());
    }
    return inputStream.read(bytes, offset, length);
  }

  @Override
  public int write(byte[] bytes, int offset, int length) throws IOException {
    if (outputStream == null) {
      outputStream = new FileOutputStream(getPipeName());
    }
    outputStream.write(bytes, offset, length);
    return length;
  }
  
  @Override
  public void close() throws IOException {
    if (outputStream == null) {
      // this ensures that we can terminate an empty load.
      // Note: Blocks if there are no readers!
      outputStream = new FileOutputStream(getPipeName()); 
    }
    //log.debug(String.format("closing output stream named pipe %s", getPipeName()));
    outputStream.close();
    if (inputStream != null) {
      //log.debug(String.format("closing input stream named pipe %s", getPipeName()));
      inputStream.close();
    }
    //log.debug("deleting FIFO " + getPipeName());
    File file = new File(getPipeName());
    if (file.exists()) {
      file.delete();
    }
    outputStream = null;
    inputStream = null;
  }
}