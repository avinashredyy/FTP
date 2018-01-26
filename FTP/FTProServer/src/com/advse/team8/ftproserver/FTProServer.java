package com.advse.team8.ftproserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class FTProServer {
    public static void main(String[] args) {
        // 0 - 1023 are reserved ports
        final int PORT_NUMBER = 1234;
        // Create a server socket
        ServerSocket controlConnection = null;
        // Socket for listening to the client
        Socket controlChannel = null;
        // Socket for listening to the client
        ServerSocket dataConnection = null;
        try {
            controlConnection = new ServerSocket(PORT_NUMBER);
            dataConnection = new ServerSocket(PORT_NUMBER+1);
            System.out.println(controlConnection);
            while(true) {
                // Listen for and accept a new connection request
                controlChannel = controlConnection.accept();
                System.out.println("Received connection request from client : "+controlChannel);
                System.out.println("Connection established to client ["+controlChannel.getInetAddress()+"]");
                // Create a new thread on the server to handle this client
                new FTProServerThread(controlChannel, dataConnection).start();
            }
        } catch(IOException ioException) {
            System.err.println(ioException.getMessage());
        }
    }
}