package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

public class App {

    final static String PIPE_NAME = "transformToXml";

    private static String transformToXml(MyModel myModel) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(MyModel.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(myModel, sw);
        String xmlContent = sw.toString();
        return xmlContent;
    }

    private static String transformToXml(String jsonString) throws JsonProcessingException {
        IpcWrapper ipcWrapper = new IpcWrapper();
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            MyModel mappedMyModel = objectMapper.readValue(jsonString, MyModel.class);
            String xmlString = transformToXml(mappedMyModel);
            ipcWrapper.setData(xmlString);
        } catch (Exception ex) {
            ipcWrapper.setHasError(true);
            ipcWrapper.setError(ex.getMessage());
        }

        String ipcWrapperJson = objectMapper.writer().writeValueAsString(ipcWrapper);
        return ipcWrapperJson;
    }

    private static void StartTcpSocketServer() throws IOException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(1234);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.US_ASCII));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String jsonString = in.readLine();
                String xmlString = transformToXml(jsonString);

                out.println(xmlString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    private static void StartUnixSocketServer() throws IOException {
        Path path = Paths.get("/tmp/CoreFxPipe_" + PIPE_NAME);
        Files.deleteIfExists(path);

        UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(path);
        ServerSocketChannel serverChannel = ServerSocketChannel
                .open(StandardProtocolFamily.UNIX);
        serverChannel.bind(socketAddress);

        final int MAX_BUFFER_SIZE = 1024;

        while (true) {
            SocketChannel channel = serverChannel.accept();

            ByteBuffer readBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
            int readLength = channel.read(readBuffer);
            byte[] readBytes = new byte[readLength];
            readBuffer.flip();
            readBuffer.get(readBytes);

            String jsonString = new String(readBytes, 0, readLength);
            String xmlString = transformToXml(jsonString);

            ByteBuffer writeBuffer = ByteBuffer.allocate(xmlString.length());
            writeBuffer.clear();
            writeBuffer.put(xmlString.getBytes());
            writeBuffer.flip();

            channel.write(writeBuffer);
            channel.close();
        }
    }

    private static void StartPipeServerWin() {
        try {
            while (true) {
                HANDLE hNamedPipe = Kernel32.INSTANCE.CreateNamedPipe(PIPE_NAME,
                        WinBase.PIPE_ACCESS_DUPLEX,
                        WinBase.PIPE_TYPE_BYTE | WinBase.PIPE_READMODE_BYTE | WinBase.PIPE_WAIT,
                        1,
                        Byte.MAX_VALUE,
                        Byte.MAX_VALUE,
                        1000,
                        null);

                Kernel32.INSTANCE.ConnectNamedPipe(hNamedPipe, null);

                final int MAX_BUFFER_SIZE = 1024;
                byte[] readBuffer = new byte[MAX_BUFFER_SIZE];
                IntByReference lpNumberOfBytesRead = new IntByReference(0);
                Kernel32.INSTANCE.ReadFile(hNamedPipe, readBuffer, readBuffer.length, lpNumberOfBytesRead, null);

                String jsonString = new String(readBuffer, StandardCharsets.US_ASCII);
                String xmlString = transformToXml(jsonString);

                byte[] writeBuffer = xmlString.getBytes(StandardCharsets.US_ASCII);
                int writeSize = writeBuffer.length;
                IntByReference lpNumberOfBytesWritten = new IntByReference(0);
                Kernel32.INSTANCE.WriteFile(hNamedPipe, writeBuffer, writeSize, lpNumberOfBytesWritten, null);
                Kernel32.INSTANCE.CloseHandle(hNamedPipe);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void StartPipeServerUnix() throws IOException {
        final int MAX_BUFFER_SIZE = 1024;
        
        while (true) {
            LinuxNamedPipe linuxNamedPipe = new LinuxNamedPipe(PIPE_NAME, true);
            byte[] readBuffer = new byte[MAX_BUFFER_SIZE];
            linuxNamedPipe.read(readBuffer, 0, MAX_BUFFER_SIZE);

            String jsonString = new String(readBuffer, StandardCharsets.US_ASCII);
            String xmlString = transformToXml(jsonString);

            byte[] writeBuffer = xmlString.getBytes(StandardCharsets.US_ASCII);
            int writeSize = writeBuffer.length;
            linuxNamedPipe.write(writeBuffer, 0, writeSize);
            linuxNamedPipe.close();
        }
    }

    public static void main(String[] args) throws IOException {
        // StartTcpSocketServer();
        StartUnixSocketServer();
        // StartPipeServerWin();
        // StartPipeServerUnix();
    }
}

@XmlRootElement
class MyModel {
    @XmlElement
    private String myProperty;

    public MyModel() {
    }

    public String getMyProperty() {
        return this.myProperty;
    }
}

class IpcWrapper {
    private String data;
    private boolean hasError;
    private String error;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean getHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}