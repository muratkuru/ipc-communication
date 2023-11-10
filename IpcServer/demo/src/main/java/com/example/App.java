package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class App {
    private static String transformToXml(MyModel myModel) {
        try
        {
            JAXBContext jaxbContext = JAXBContext.newInstance(MyModel.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            StringWriter sw = new StringWriter();
            jaxbMarshaller.marshal(myModel, sw);
            String xmlContent = sw.toString();
            return xmlContent;
   
        } catch (JAXBException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(1234);
            ObjectMapper objectMapper = new ObjectMapper();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.US_ASCII));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                
                String jsonString = in.readLine();
                MyModel myModel = objectMapper.readValue(jsonString, MyModel.class);
                String xmlString = transformToXml(myModel);

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
