/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.advse.team8.ftproclient.controller;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 *
 * @author ganesh
 */
public class FTProClientController {
    private final int dataPort = 1235;
    private DataOutputStream outputStream = null;
    // To get FTP commands from the client
    Scanner scanner = new Scanner(System.in);
    // Socket for transferring files 
    Socket dataChannel = null;
    String currentDirectory = "/home/ganesh/ftpclientDownloads/";
    String fileType = "A";
    
    public FTProClientController(DataOutputStream outputStream){
        this.outputStream = outputStream;
    }
    
    public String processServerReply(String commandToServer, String replyFromServer) throws IOException{
        System.out.println("Hello");
        String remoteFileName = "";
        if (replyFromServer==null || replyFromServer.isEmpty()){
            return "";
        } 
        if(!commandToServer.isEmpty() && commandToServer.contains("RETR")){
            String[] retrWords = commandToServer.split(" ");
            if (retrWords.length == 2) {
                remoteFileName = retrWords[1];
            }
        }
        if (replyFromServer.contains("200")) {
            if (replyFromServer.contains("TYPE")) {
                System.out.println("PASV or PORT ??");
                commandToServer = scanner.nextLine();
                outputStream.writeUTF(commandToServer);
                return commandToServer;
            }
            System.out.println("Type and enter to send FTP commands to send to server.");
            // This command should be USER
            commandToServer = scanner.nextLine();
            outputStream.writeUTF(commandToServer);
        } else if (replyFromServer.contains("227")) {
            System.out.println("STOR or RETR ??");
            commandToServer = scanner.nextLine();
            outputStream.writeUTF(commandToServer);
            // Connect to the server's data channel to intiate the file transfer
            dataChannel = new Socket("localhost", dataPort);
        } else if (replyFromServer.contains("125")) {
            if (dataChannel!=null) {
                System.out.println("Server is ready to receive the file. Enter the local file path (E.g:/localpath/file) to send");
                String localFileName = scanner.nextLine();

                if (!localFileName.isEmpty()) {
                    FileInputStream fileInputStream = null;
                    OutputStream dataChannelOutputStream = null;
                    DataOutputStream doutputStream = null;
                    try{
                        File fileobj = null;
                        while (true) {
                            fileobj = new File(localFileName);
                            if (fileobj.isDirectory()) {
                                System.err.println("Directory can't be uploaded!!");
                                System.out.println("Enter valid local file path (E.g:/localpath/file)");
                                localFileName = scanner.nextLine();
                            } else if(!fileobj.exists()){
                                System.err.println("File doesn't exist!");
                                System.out.println("Enter valid local file path (E.g:/localpath/file)");
                                localFileName = scanner.nextLine();
                            } else break;
                        }
                        if (fileType.equals("I")) {
                            fileInputStream = new FileInputStream(fileobj);
                            dataChannelOutputStream = dataChannel.getOutputStream();
                            byte[] buffer = new byte[dataChannel.getSendBufferSize()];
                            System.out.println("length of send buffer = "+buffer.length);
                            int bytesRead = 0;
                            while((bytesRead = fileInputStream.read(buffer))>0)
                            {
                                System.out.println("bytesRead = "+bytesRead);
                                dataChannelOutputStream.write(buffer,0,bytesRead);
                            }
                        } else {
                            byte[] encoded = Files.readAllBytes(Paths.get(localFileName));
                            String fileContents = new String(encoded, Charset.forName("US-ASCII"));
                            doutputStream = new DataOutputStream(dataChannel.getOutputStream());
                            doutputStream.writeUTF(fileContents);
                        }
                        
                    } catch(FileNotFoundException fnotex){
                         System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
                         commandToServer = scanner.nextLine();
                         outputStream.writeUTF(commandToServer);
                         
                    } finally{
                        if (fileInputStream!=null) fileInputStream.close();
                        if (dataChannelOutputStream!=null) dataChannelOutputStream.close();
                        if (doutputStream!=null) doutputStream.close();
                        if (dataChannel!=null) dataChannel.close();
                    }
                    
                }
            }   
        } else if (!commandToServer.isEmpty() && commandToServer.contains("STOR") && replyFromServer.contains("226")) {
            System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
            commandToServer = scanner.nextLine();
            if (commandToServer.contains("TYPE")) {
                this.fileType = commandToServer.split(" ")[1];
            }
            outputStream.writeUTF(commandToServer);
        } else if (!commandToServer.isEmpty() && commandToServer.contains("RETR") && replyFromServer.contains("226")) {
            System.out.println("Starting to read file contents from data channel");
            InputStream dataChannelInputStream = dataChannel.getInputStream();
            File file = new File(currentDirectory + remoteFileName);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] buffer = new byte[dataChannel.getReceiveBufferSize()];
            System.out.println("length of receive buffer = "+buffer.length);
            System.out.println("Downloading file from server");
            int bytesRead = 0;
            while ((bytesRead = dataChannelInputStream.read(buffer)) != -1) {
                System.out.println("bytesRead = "+bytesRead);
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.close();
            dataChannelInputStream.close();
            dataChannel.close();
            System.out.println("File downloaded successfully");
            commandToServer = "Download completed";
            outputStream.writeUTF(commandToServer);
            System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
            commandToServer = scanner.nextLine();
            outputStream.writeUTF(commandToServer);
        } else {
            System.out.println("Continue to send more FTP commands or TYPE QUIT to disconnect");
            commandToServer = scanner.nextLine();
            if (commandToServer.contains("TYPE")) {
                this.fileType = commandToServer.split(" ")[1];
            }
            outputStream.writeUTF(commandToServer);
        }
        return commandToServer;
    }
}
