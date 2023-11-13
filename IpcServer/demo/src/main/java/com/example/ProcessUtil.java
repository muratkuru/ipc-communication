package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessUtil {
  
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * take all data from an input stream and put it in a String.
   * @param inputStream
   * @return
   */
  private static String getCmdOutput(InputStream inputStream) {
    StringBuffer buf = new StringBuffer();
    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
    try {
      String line = in.readLine();
      while (line != null) {
        buf.append(line);
        buf.append(LINE_SEPARATOR);
      }
    } catch (IOException e) {
      return "Operation failed";
    }
    return buf.toString();
  }

  /**
   * Run a system command.
   * 
   * @param commandWithArgs the command to run, with arguments
   * @throws IOException if the command failed
   */
  public static void runCommand(String[] commandWithArgs) throws IOException {
    Process proc = Runtime.getRuntime().exec(commandWithArgs);
    int status;
    try {
      status = proc.waitFor();
    } catch (InterruptedException inte) {
      String cmdOutput = getCmdOutput(proc.getErrorStream());
      throw new IOException(commandWithArgs[0] + " interrupted: " + cmdOutput);
    }
    if (status != 0) {
      String cmdOutput = getCmdOutput(proc.getErrorStream());
      throw new IOException(commandWithArgs[0] + " failed: " + cmdOutput);
    }
  }
}