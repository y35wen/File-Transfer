//receiver program
// command line inputs: <hostname for the network emulator>
//	<UDP port number used by the link emulator to receive ACKs from the receiver>
// 	<UDP port number used bt the receiver to receive data from the emulator>
// 	and <name of the file into which the received data is written>

import java.io.*;
import java.net.*;
import java.util.Arrays;

class receiver {

	public static void main(String args[]) throws Exception{

		// check arguments
		if(args.length!=4){
			System.err.println("4 command line arguments");
			System.exit(1);
		}

		//fields
        String emu_host_name = args[0]; // command line arguments
        InetAddress address = InetAddress.getByName(emu_host_name);
        String emuPort_str = args[1];
        int emuPort = Integer.parseInt(emuPort_str);  
        String receiverPort_str = args[2];
        int receiverPort = Integer.parseInt(receiverPort_str); 
        String fileName =args[3];

        int seqMod = 32;
		DatagramSocket receiveSocket = new  DatagramSocket(receiverPort);
        DatagramSocket sendSocket = new DatagramSocket();
		PrintWriter outputWriter = new PrintWriter(fileName, "UTF-8");  // output file
        PrintWriter arrivalWriter = new PrintWriter("arrival.log", "UTF-8"); // arrival.log file

        int expSeq = 0;
        int last = 0;
        while(1==1){
            // STEP 1: receiving packets sent by the sender via the emulator
        	byte[] receiveData = new byte[1024];
        	DatagramPacket buffer = new DatagramPacket(receiveData,receiveData.length);
        	receiveSocket.receive(buffer);
        	packet receive_packet = packet.parseUDPdata(receiveData);
        	int receivedSeq = receive_packet.getSeqNum();
        	int receive_packet_type = receive_packet.getType();

        	// STEP 2: check the sequence number of the packet, if the sequence number is
        	//	the one, it should send an ACK packet back to the sender with the 
        	// 	sequence num equal to the sequence number of the received packet
        	if(expSeq==receivedSeq){  
        		expSeq = (expSeq+1) % seqMod;
                last = receivedSeq % seqMod;
        		arrivalWriter.println(receivedSeq);
        		if(receive_packet_type==2){
        			packet ack = packet.createEOT(receivedSeq%seqMod);
        			byte[] send = ack.getUDPdata();
        			DatagramPacket p = new DatagramPacket(send, send.length,address,emuPort);
        			sendSocket.send(p);
        			arrivalWriter.close();
        			outputWriter.close();
                    sendSocket.close();
                    receiveSocket.close();
        			break;
        		} 
        		else {
                    outputWriter.print(new String (receive_packet.getData()));
        			packet ack = packet.createACK(receivedSeq%seqMod);
        			byte[] send = ack.getUDPdata();
        			DatagramPacket p = new DatagramPacket(send, send.length,address,emuPort);
        			sendSocket.send(p);
        		}
        	}

        	// STEP 3: in all other cases, it should discard the received pkt and resend 
        	//	 the most ACK for the most recently received-in-order pkt
        	else {
                arrivalWriter.println(receivedSeq);
        		packet ack = packet.createACK(last);
        		byte[] send = ack.getUDPdata();
        		DatagramPacket sendBuff = new DatagramPacket(send, send.length,address, emuPort);
       			sendSocket.send(sendBuff);
        	}
        }
	}
}