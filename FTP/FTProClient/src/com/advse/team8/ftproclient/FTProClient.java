/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.advse.team8.ftproclient;

import com.advse.team8.ftproclient.controller.FTProClientController;
import java.net.Socket;
import java.io.*;
import java.util.Scanner;

public class FTProClient {
    public static void main(String[] args) {
  
        // 0 - 1023 are reserved ports
        final int PORT_NUMBER = 1234;
        // Socket for listening to the client 
        Socket controlChannel = null;
        
        // Input stream to receive information from server
        DataInputStream inputStream = null;
        // Output stream to send information to server
        DataOutputStream outputStream = null;
       
        try {
            controlChannel = new Socket("localhost", PORT_NUMBER);
            inputStream = new DataInputStream(controlChannel.getInputStream());
            outputStream = new DataOutputStream(controlChannel.getOutputStream());
            System.out.println(controlChannel);
            // Send/Receive data to and from server
            FTProClientController ftproClientController = new FTProClientController(outputStream);
                    
            String commandToServer = "";
            while(true) {
                String replyFromServer = inputStream.readUTF();
                if (!replyFromServer.isEmpty()) {
                    int status = 401;
                    try {
                        status = Integer.parseInt(replyFromServer.split(" ")[0]);
                    } catch (NumberFormatException nfe){}
                    if (status >= 400) System.err.println("Server : "+replyFromServer);
                    else System.out.println("Server : "+replyFromServer);
                }
                System.out.println("askjdnasdj");
                commandToServer = ftproClientController.processServerReply(commandToServer, replyFromServer);
            }
        } catch (IOException ioException) {
            System.err.println(ioException.getMessage());
        } finally {
            try {
                if(inputStream!=null) inputStream.close();
                if(outputStream!=null) outputStream.close();
                if(controlChannel!=null) controlChannel.close();
            } catch (IOException ioException){
                // Suppress the message. Unable to close the sockets but JVM will take care of it though!!
            }
        }
    }
}
