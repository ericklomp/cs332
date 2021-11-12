package cs332.cs332.RCMP;
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
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
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

/*


    
*/
public class Sender {
    private int port = 22222;
	private InetAddress destAddress;
    private String filename = "";
    private DatagramSocket socket;
    public final static int HEADER = 13;
    public final static int PAYLOADSIZE = 1450;
    public final static int FULLPCKT = HEADER + PAYLOADSIZE;
    public final static int FILE_SIZE_LENGTH = 4;
    public final static int CONNECTION_ID = 4;
    public final static int PACKET_NUM_LENGTH = 4;
    /*
    * This is the main of the project  
    */
   public static void main(String[] args) throws NumberFormatException, SocketException{
        //System.out.println(args[0] + " " + Integer.parseInt(args[1]) + " " + args[2]);
        Sender sender = new Sender();
        sender.setDestinationAddress(args[0]);
        sender.parseArgs(args);
        sender.initSocket();
        sender.run();
   }

   /*
   * Param: String address
   * sets up the destingation address 
   */
   public void setDestinationAddress(String address) {
        try {
            this.destAddress = InetAddress.getByName(address);
            System.out.println(destAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
   /*
   * Param: String[] args
   * parses the commandline to give port number and file name
   */
    public void parseArgs(String[] args) {
        this.port = Integer.parseInt(args[1]);
        this.filename = args[2];
        System.out.println(this.port + " " + this.filename);
    }

  /*
   * @throws SocketException
   * initalizes the socket object
   */
    public void initSocket() throws SocketException{
        this.socket = new DatagramSocket();
    }

  /*
   *  @throws FileNotFoundException, IOException
   *  main executable of the project. This Does all of the heavy work setting everything up
   *  and sending the packets
   */
    public void run(){
        try{
            //get file info
            File file = new File(this.filename);
            long size = file.length();

            //number of packets expected
            int totalPackets = (int)(((double)size / (double)PAYLOADSIZE) + .5);
            System.out.println("Expected Packets: " + totalPackets);

            //setting up the chunks and packet to be send
            byte[] chunks = new byte[PAYLOADSIZE];
            byte[] pckt = new byte[FULLPCKT];
            ByteBuffer packet = ByteBuffer.wrap(pckt);
            //opening the file
            RandomAccessFile fin = new RandomAccessFile(file, "r");
            int chunkLength = 0;
            int position = 0;
            int amount = 0;
            int packetID = 0;
            int connectionID = new Random().nextInt(Integer.MAX_VALUE);
            int lastPacket = 0;
            int gap = 0;
            int gapCounter =0;
            byte Ack = 1;
            int packetsSent = 0;
            int lostpackets = 0;
            boolean resend = false;
            boolean timeOut = false;
            int bytesAcked = 0;
            int ackPcktID = 0;

            while(true){
                if (resend) {
                    Ack = 1;
                }
                else 
                    Ack = (byte) (packetsSent == gap ? 1 : 0);

                packet.clear();

                //read in the file
                chunkLength = fin.read(chunks);
                if(chunkLength == -1) break;
                if (packetID == totalPackets - 1){
                    Ack = 1;
                }

                //create header
                packet.putInt(connectionID);
                packet.putInt(amount);
                packet.putInt(packetsSent);
                packet.put(Ack);
                packet.put(chunks);
                
                //send packet
                UdpSend(Arrays.copyOfRange(packet.array(), 0, chunkLength + HEADER));
                if (Ack == 1) {
                    ackPcktID = packetID;
                }
                packetsSent++;
                position += chunkLength;

                //wait for an ack to be send back
                if (Ack == 1){
                    System.out.println("Receiving ACK");
                    try{
                        //receive ack
                        receiveACK();
                    }
                    catch (SocketTimeoutException e){
                        //if there is a timeout reset some of the data
                        System.out.println("Timeout");
                        timeOut = true;
                        gap = gapCounter = 0;
                        packetID = lastPacket;
                        position = bytesAcked;
                        fin.seek(bytesAcked);
                        resend = true;
                        break;
                    }

                    //sent ack was a success so update info
                    bytesAcked = position - chunkLength;
                    resend = false;
                    timeOut = false;
                    gapCounter++;
                    gap += gapCounter;
                    System.out.println("Gaps counted: " + gapCounter);
                }
                //System.out.println("Waiting for ACK");
                if(!timeOut){
                    packetID++;
                }
            }
            fin.close();
        } catch(FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  /*
   * Param: Byte[] bytes
   * sets up the packet to be created 
   */
    private DatagramPacket createPacket(byte[] bytes) {
        return new DatagramPacket(bytes, bytes.length, this.destAddress, this.port);
    }

  /*
   * Param: Bytes[] byte
   * sends the data using udp
   */
    private void UdpSend(byte[] buffer){
        try{
            this.socket.send(createPacket(buffer));
            System.out.println("PacketSent");
        } catch(IOException e){
            e.printStackTrace();
        }
    }

  /*
   * Param @ throws IOException
   * receives the ack sent from the receiver and prints out the contents of the ack 
   */
    private void receiveACK() throws IOException{
        byte[] ack = new byte[CONNECTION_ID + PACKET_NUM_LENGTH];
        DatagramPacket ackpacket = createPacket(ack);
        socket.receive(ackpacket);
        ByteBuffer byteBuffer = ByteBuffer.wrap(ack);
        int connectionID = byteBuffer.getInt();
        int lastPacketRecieved = byteBuffer.getInt();
        System.out.println("Received ACK of Packets #:" + lastPacketRecieved + " On: " + connectionID);
        System.out.println("ACK");
        for(byte bytes: byteBuffer.array()){
            System.out.println("" + bytes + " ");
        }
        //return byteBuffer;
        //int receiveACK = byteBuffer.getInt();
    }
}