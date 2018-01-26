package com.advse.team8.ftproserver.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class FTProServerController {
    private DataOutputStream outputStream = null;
    private ServerSocket dataConnection = null;
    private Socket dataChannel = null;
    private final static int DATA_PORT = 1235;
    private String currentDirectory = "";
    private String clientName = "";
    private String currentUser = ""; 
    private Map<String,String> users = null;
    private boolean loggedIn = false;
    private String fileType = "A";
    
    public FTProServerController(ServerSocket dataConnection, DataOutputStream outputStream, String clientName){
        this.dataConnection = dataConnection;
        this.outputStream = outputStream;
        this.currentDirectory = "/home/ganesh/ftpserverRoot/";
        this.clientName = clientName;
        Map<String,String> users = new HashMap<String, String>();
        users.put("ganesh", "ganesh123");
        users.put("gokul", "gokul123");
        users.put("avinash", "avinash123");
        this.users = users;
    }
    
    public boolean processCommand(String commandFromClient) throws IOException{
        if (!loggedIn) {
            System.out.println("Client ["+clientName+",user=anonymous]: "+commandFromClient);    
        } else {
            System.out.println("Client ["+clientName+",user="+this.currentUser+"]: "+commandFromClient); 
        }
        String msgToClient = "";
        if(commandFromClient==null || commandFromClient.isEmpty()){
        } else if (commandFromClient.contains("STOR") && !loggedIn){
            outputStream.writeUTF("532 Need account for storing files.!");
        } else if (commandFromClient.contains("USER")){
            String userName = commandFromClient.split(" ")[1];
            if (users.containsKey(userName)){
                msgToClient = "331 User name ok; Need password for "+userName;
                if (!this.currentUser.isEmpty()) {
                    System.out.println(this.currentUser + "logged out!");
                }
                this.currentUser = userName;
                this.loggedIn = false;
                outputStream.writeUTF(msgToClient);
            } else {
                msgToClient = "332 Need account for login.";
                this.currentUser = userName;
                outputStream.writeUTF(msgToClient);
            }
        } else if (commandFromClient.contains("PASS")){
            String password = commandFromClient.split(" ")[1];
            String actualpwd = this.users.get(this.currentUser);
            if (actualpwd.equals(password)){
                msgToClient = "230 User logged in, proceed";
                this.loggedIn = true;
                outputStream.writeUTF(msgToClient);
            } else {
                msgToClient = "530 User not logged in.";
                this.loggedIn = true;
                outputStream.writeUTF(msgToClient);
            }
        } else if (!loggedIn){
            outputStream.writeUTF("Login to proceed!");
        } else if (commandFromClient.equalsIgnoreCase("PWD")){
            outputStream.writeUTF("200 " +this.currentDirectory);
        } else if (commandFromClient.contains("CWD")){
            String directory = commandFromClient.split(" ")[1];
            File fileobj = new File(directory);
            if(fileobj.isDirectory()){
                this.currentDirectory = directory;
                outputStream.writeUTF("200 Changed to "+this.currentDirectory);
            } else {
                outputStream.writeUTF("550 Requested action not taken. Invalid path!");
            }
        } else if (commandFromClient.equalsIgnoreCase("LIST")) {
            File folder = new File(this.currentDirectory);
            File[] listOfFiles = folder.listFiles();
            StringBuffer dirs = new StringBuffer();
            StringBuffer files = new StringBuffer();
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    files.append("   "+listOfFile.getName() + "\n");
                } else if (listOfFile.isDirectory()) {
                    dirs.append("++ "+listOfFile.getName()+"\n");
                }
            }
            outputStream.writeUTF("200 Command LIST okay. \n"+dirs.toString() + files.toString());
        } else if (commandFromClient.contains("TYPE")) {
            fileType = commandFromClient.split(" ")[1];
            msgToClient = "200 TYPE Command okay. Set to ASCII ";
            if (fileType.equals("I")){
                msgToClient = "200 TYPE Command okay. Set to image/binary.";
            }
            outputStream.writeUTF(msgToClient);
        } else if (commandFromClient.equalsIgnoreCase("PASV")) {
            msgToClient = "227 Entering Passive Mode (127,0,0,1,"+this.DATA_PORT+")";
            outputStream.writeUTF(msgToClient);
            // Establish data channel for file transfer
            this.dataChannel = this.dataConnection.accept();
            System.out.println("Data channel established "+dataChannel);    
        } else if (commandFromClient.contains("STOR")) {
            String[] storWords = commandFromClient.split(" ");
            if (storWords.length == 2) {
                String fileName = storWords[1];
                msgToClient = "125 Data connection already open; transfer starting.";
                outputStream.writeUTF(msgToClient);
                // Wait and receive the actual file contents from the client
                File file = new File(this.currentDirectory + fileName);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                //System.out.println("Waiting for a file from client ["+this.clientName+"]");
                if (fileType.equals("I")) {
                    
                    InputStream dataChannelInputStream = this.dataChannel.getInputStream();
                    System.out.println("Waiting for binary file from client ["+this.clientName+"]");
                    byte[] buffer = new byte[this.dataChannel.getReceiveBufferSize()];
                    System.out.println("length of receive buffer = "+buffer.length);
                
                    while (true) {
                        int bytesRead = 0;
                        boolean fileRead = false;
                        while ((bytesRead = dataChannelInputStream.read(buffer)) != -1) {
                            System.out.println("bytesRead = "+bytesRead);
                            fileOutputStream.write(buffer, 0, bytesRead);
                            fileRead = true;
                        }
                        fileOutputStream.close();
                        if(fileRead) break;
                    }
                    dataChannelInputStream.close();
                    
                } else {
                    System.out.println("Waiting for ascii file from client ["+this.clientName+"]");
                    DataInputStream dinputStream = new DataInputStream(this.dataChannel.getInputStream());
                    String fileContents = dinputStream.readUTF();
                    fileOutputStream.write(fileContents.getBytes(Charset.forName("US-ASCII")));
                    fileOutputStream.close();
                }
                
                // Send the upload success message to the client
                msgToClient = "226 Closing data connection. Requested file action (Upload) successful.";
                outputStream.writeUTF(msgToClient);
                this.dataChannel.close();
            }
        } else if (commandFromClient.contains("RETR")) {
            String[] retrWords = commandFromClient.split(" ");
            if (retrWords.length == 2) {
                String localFileName = retrWords[1];
                if (!localFileName.isEmpty()) {
                    OutputStream dataChannelOutputStream = null;
                    try {
                        File fileobj = new File(this.currentDirectory + localFileName);
                        if (fileobj.isDirectory()) {
                            msgToClient = "450 Requested file action not taken. Directory upload is not supported by FTP";
                            outputStream.writeUTF(msgToClient);
                        } else {
                            File fileobj1 = new File(this.currentDirectory + localFileName);
                            FileInputStream fileInputStream = new FileInputStream(fileobj1);
                            dataChannelOutputStream = dataChannel.getOutputStream();
                            byte[] buffer = new byte[dataChannel.getSendBufferSize()];
                            System.out.println("length of receive buffer = "+buffer.length);
                            int bytesRead = 0;
                            while((bytesRead = fileInputStream.read(buffer))>0)
                            {
                                System.out.println("bytesRead = "+bytesRead);
                                dataChannelOutputStream.write(buffer,0,bytesRead);
                            }
                            fileInputStream.close();
                            // Send the upload success message to the client
                            msgToClient = "226 Closing data connection. Requested file action (Download) successful. Start reading!";
                            outputStream.writeUTF(msgToClient);
                        }
                    } catch(FileNotFoundException fnotex){
                        msgToClient = "450 Requested file action not taken. "+localFileName+" doesn't exist in the "+this.currentDirectory;
                        outputStream.writeUTF(msgToClient);
                    } finally {
                        if (dataChannelOutputStream!=null) dataChannelOutputStream.close();
                        if (dataChannel!=null) dataChannel.close();
                    }

                }
            }
        } else if (commandFromClient.equals("QUIT")){
            return false;
        }
        return true;
    }
}
