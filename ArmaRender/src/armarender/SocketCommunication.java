package armarender;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class SocketCommunication extends Thread {
    public boolean running = true;
    private Vector<String> outCommands;
    private Vector<String> inCommands;
    
    public SocketCommunication(){
        System.out.println("SocketCommunication");
        outCommands = new <String>Vector();
        inCommands = new <String>Vector();
    }
    
    
    public void sendCommand(String command){
        outCommands.addElement(command);
    }
    
    //public void
    
    //public

    public void run(){
        try {
            ServerSocket serverSocket = new ServerSocket(4343, 10);
            Socket socket = serverSocket.accept();
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            while(running){
                
                if(socket.isConnected() == false || socket.isClosed() == true){ // reconnect
                    Thread.sleep(500);
                    System.out.println("Reconnecting.");
                    try {
                        socket = serverSocket.accept();
                        is = socket.getInputStream();
                        os = socket.getOutputStream();
                    } catch (Exception e){
                        System.out.println("A Error " + e);
                        socket.close();
                    }
                }
                
                if(socket.isConnected() == true || socket.isClosed() == false){
                    try {
                        // Receiving
                        byte[] lenBytes = new byte[4];
                        is.read(lenBytes, 0, 4);
                        int len = (((lenBytes[3] & 0xff) << 24) | ((lenBytes[2] & 0xff) << 16) |
                                  ((lenBytes[1] & 0xff) << 8) | (lenBytes[0] & 0xff));
                        byte[] receivedBytes = new byte[len];
                        is.read(receivedBytes, 0, len);
                        String received = new String(receivedBytes, 0, len);

                        System.out.println("Server received: " + received);

                        
                        
                        // Sending
                        String toSend = "noop";
                        
                        if(outCommands.size() > 0){
                            toSend = outCommands.elementAt(0);
                            outCommands.removeElementAt(0);
                        }
                        
                        
                        byte[] toSendBytes = toSend.getBytes();
                        int toSendLen = toSendBytes.length;
                        byte[] toSendLenBytes = new byte[4];
                        toSendLenBytes[0] = (byte)(toSendLen & 0xff);
                        toSendLenBytes[1] = (byte)((toSendLen >> 8) & 0xff);
                        toSendLenBytes[2] = (byte)((toSendLen >> 16) & 0xff);
                        toSendLenBytes[3] = (byte)((toSendLen >> 24) & 0xff);
                        os.write(toSendLenBytes);
                        os.write(toSendBytes);
                    } catch (Exception e){
                        System.out.println("B Error " + e);
                        socket.close();
                    }
                
                }
                Thread.sleep(100);
            }
                
            socket.close();
            serverSocket.close();
            System.out.println("server closed.");
        } catch (Exception e){
            System.out.println("C Error " + e);
        }
    }
    
    
    
    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(4343, 10);
        Socket socket = serverSocket.accept();
        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();

        // Receiving
        byte[] lenBytes = new byte[4];
        is.read(lenBytes, 0, 4);
        int len = (((lenBytes[3] & 0xff) << 24) | ((lenBytes[2] & 0xff) << 16) |
                  ((lenBytes[1] & 0xff) << 8) | (lenBytes[0] & 0xff));
        byte[] receivedBytes = new byte[len];
        is.read(receivedBytes, 0, len);
        String received = new String(receivedBytes, 0, len);

        System.out.println("Server received: " + received);

        // Sending
        String toSend = "Echo: " + received;
        byte[] toSendBytes = toSend.getBytes();
        int toSendLen = toSendBytes.length;
        byte[] toSendLenBytes = new byte[4];
        toSendLenBytes[0] = (byte)(toSendLen & 0xff);
        toSendLenBytes[1] = (byte)((toSendLen >> 8) & 0xff);
        toSendLenBytes[2] = (byte)((toSendLen >> 16) & 0xff);
        toSendLenBytes[3] = (byte)((toSendLen >> 24) & 0xff);
        os.write(toSendLenBytes);
        os.write(toSendBytes);

        socket.close();
        serverSocket.close();
    }
     
}
