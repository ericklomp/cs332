package cs332.RCMP;

/*
*
* Reciever Side of Reliable Message-based Protocol for File Trasfer Using UDP
* Author: Eric Klomp edk22
* Date: 10/19/2021
*
* Everything up to part 7 should be done
*/
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class Receiver{
    private String filename = "";
    private Integer port = 22222;
    private DatagramSocket socket;
    private FileOutputStream fout;
    public final static int PAYLOADSIZE = 1450;
    public final static int HEADER = 13;
    public final static int FULLPCKT = HEADER + PAYLOADSIZE;
    public final static int CONNECTION_ID = 4;
    public final static int PACKET_NUM = 4;
    public static void main(String[] args) throws NumberFormatException, SocketException{
        System.out.println(Integer.parseInt(args[0]) + " " + args[1]);
        Receiver receiver = new Receiver();
        receiver.parseArgs(args);
        receiver.initSocket();
        receiver.run();
   }

  /*
   * Param: String address
   * sets up the destingation address 
   */
   public void initSocket() throws SocketException{
        this.socket = new DatagramSocket(this.port);
    }
  /*
   * Param: String[] args
   * parses the commandline to give port number and file name
   */
    public void parseArgs(String[] args) {
        this.port = Integer.parseInt(args[0]);
        this.filename = args[1];
    }

  /*
   *  @throws FileNotFoundException, IOException
   *  main executable of the project. This Does all of the heavy work setting everything up
   *  for receiving the file and sending the ACKS
   */
    public void run(){
        int lastPacketRecieved = 0;
        byte[] receiveData = new byte[FULLPCKT];
        DatagramPacket newPacket;
        boolean time = true;
        int lastPacket = 0;
        int bytesAcked = 0;
        System.out.println("Receiving file... ");
        try(RandomAccessFile fout = new RandomAccessFile(this.filename, "rw")){
            System.out.print("File Opened");
            while(true) {
                //Receiving all the data
                newPacket = new DatagramPacket(receiveData, FULLPCKT);
                socket.receive(newPacket);
                byte[] payload = Arrays.copyOfRange(newPacket.getData(), HEADER, newPacket.getLength());
                ByteBuffer header = ByteBuffer.wrap(Arrays.copyOfRange(newPacket.getData(), 0, HEADER));
                int connectionID = header.getInt();
                int bytesReceived = header.getInt();
                int packetNum = header.getInt();
                int Ack = header.get();

                //last packet the was received
                if(lastPacket == packetNum){
                    lastPacket++;
                    if(Ack == 1){
                        lastPacketRecieved = packetNum;
                        bytesAcked = bytesReceived;
                    }
                    fout.seek(bytesReceived);
                }
                //something was wrong/ packet was dropped
                else{
                    lastPacket = lastPacketRecieved;
                    fout.seek(bytesAcked);
                    if (Ack == 1){
                        sendACK(Ack, newPacket.getAddress(), newPacket.getPort(), connectionID, lastPacketRecieved);
                    }
                    continue;
                }

                System.out.println("Received Data");
                System.out.println(String.format("connectionId: %d, bytesReceived: %d, packetNum: %d,", connectionID, bytesReceived, packetNum));
                fout.write(payload);
                System.out.println(payload);
                //fout.flush();
                System.out.println("Wrote to file");
                lastPacketRecieved++;
                if (Ack == 1){
                    System.out.println("Send ACK");
                    sendACK(Ack,newPacket.getAddress(), newPacket.getPort(), connectionID, lastPacketRecieved);
                }
                if (newPacket.getLength() < FULLPCKT){
                    System.out.println("Done");
                    break;
                }
            }
            fout.close();
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }  
    }

  /*
   *  @throws IOException
   *  Param: int Ack, InetAddress address, int port, int connectionID, int lastPacketRecieved
   *  Sends Ack to the sender to let it know it received the packet
   */
    private void sendACK(int Ack, InetAddress address, int port, int connectionID, int lastPacketRecieved) throws IOException {
        if (Ack == 1){
            ByteBuffer ack = ByteBuffer.wrap(new byte[8]);
            ack.clear();
            ack.putInt(connectionID);
            ack.putInt(lastPacketRecieved);
            for(byte b : ack.array()){
            System.out.println("" + b + "");
            }
            //String ack = "ACK";
            //sendACK = ack.getBytes();
            UdpSend(ack.array(), address, port);
            //this.socket.send(ackPacket);
            System.out.println("ACK Sent");
        }
        
    }

  /*
   * Param: Bytes[] byte
   * sends the data using udp
   */
    private void UdpSend(byte[] buffer, InetAddress address, int port){
        try{
            this.socket.send(createPacket(buffer, address, port));
            System.out.println("PacketSent");
        } catch(IOException e){
            e.printStackTrace();
        }
    }

  /*
   * Param: Byte[] bytes
   * sets up the packet to be created 
   */
    private DatagramPacket createPacket(byte[] bytes, InetAddress address, int port) {
        return new DatagramPacket(bytes, bytes.length, address, port);
    }
}
