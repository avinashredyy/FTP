package com.advse.team8.ftproserver;

import com.advse.team8.ftproserver.controller.FTProServerController;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class FTProServerThread extends Thread {
    private Socket controlChannel = null, dataChannel = null;
    private ServerSocket dataConnection = null;
    private String clientName = "";
    
    public FTProServerThread(Socket controlChannel, ServerSocket dataConnection) {
        this.controlChannel = controlChannel;
        this.dataConnection = dataConnection;
        this.clientName = controlChannel.getInetAddress()+":"+controlChannel.getPort();
    }

    public void run() {
        // Input stream to receive response from client
        DataInputStream inputStream = null;
         // Output stream to send information to client
        DataOutputStream outputStream = null;
        boolean notQuit = true;
        try {
            // Establish communication channels between server and client
            inputStream = new DataInputStream(controlChannel.getInputStream());
            outputStream = new DataOutputStream(controlChannel.getOutputStream());

            // Tell the client that the connection is established successfully.
            String msgToClient = "200 Connected Successfully";
            outputStream.writeUTF(msgToClient);

            // Listen and respond continously
            System.out.println("Listening for commands from the client ["+this.clientName+"]");
            
            FTProServerController ftproServerController = new FTProServerController(this.dataConnection, outputStream, this.clientName);
            
            String commandFromClient = "";
            while(notQuit) {
                commandFromClient = inputStream.readUTF();                
                notQuit = ftproServerController.processCommand(commandFromClient);
            }
            ftproServerController = null;
            
        } catch (EOFException eofException) {
            System.err.println(this.clientName+" disconnected!");
        } catch(IOException ioException) {
            System.err.println(ioException.getMessage());
        } finally {
            try {
                if(inputStream!=null) inputStream.close();
                if(outputStream!=null) outputStream.close();
                if(controlChannel!=null) controlChannel.close();
                if (!notQuit) {
                    System.err.println(this.clientName+" disconnected!");
                }
            } catch (IOException ioException){
                // Suppress the message. Unable to close the sockets but JVM will take care of it though!!
            }
        }
    }
}
