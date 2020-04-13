// sender program
// command line input: <host address of the network emulator>,
//	<UDP port # used by the emulator to receive data from sender>
//	<UDP port# used bu sender to receiver ACKs from the emulator>
//	<name of the file to be transferred>

import java.io.*;
import java.net.*;
import java.util.Arrays;

class sender {

	public static void main(String args[]) throws Exception{

		//check command line arguments
		if(args.length!=4){
			System.err.println("4 command line arguments");
			System.exit(1);
		}

		//fields
		String emuAddress = args[0];  // command line arguments
		InetAddress address =  InetAddress.getByName(emuAddress);
		String portForData_str = args[1];
		int portForData = Integer.parseInt(portForData_str);
		String portForACK_str = args[2];
		int portForACK=Integer.parseInt(portForACK_str); 
		String fileName=args[3];

		int windowSize =10;
		int pktDataSize = 500;
	 	int seqMod = 32;
		int timeOut = 100;
		DatagramSocket sendSocket = new DatagramSocket();
		DatagramSocket acksocket = new DatagramSocket(portForACK);
		PrintWriter seqnumWriter = new PrintWriter("seqnum.log", "UTF-8");  // seqnum.log file
        PrintWriter ackWriter = new PrintWriter("ack.log", "UTF-8"); // ack.log file
        
		// STEP 1: read data from the specified file:
		File file = new File(fileName);
		byte[] bytesArray = new byte[(int)file.length()];
		FileInputStream fis = new FileInputStream(file);
		fis.read(bytesArray);
		fis.close();
			// convert file into array of packets
		int pktsNum = (int)Math.ceil((double)file.length()/500);
		packet packets[] = new packet[pktsNum];
		for(int i=0;i<packets.length;i++){
			if(i!=(pktsNum-1)){
				String slice = new String(Arrays.copyOfRange(bytesArray,i*500,(i+1)*500));
				packets[i] = packet.createPacket(i, slice);
			}else{
				String last_part = new String(Arrays.copyOfRange(bytesArray,i*500,bytesArray.length));
				packets[i] = packet.createPacket(i, last_part);
			}
		}
		
		// send packets using the GO-Back-N protocaol to the receiver via the network emulator
		int total_acked = 0;  
		int send_not_acked = 0; 
		int expectACK =0;

		while(1==1){
			
			if(pktsNum==0) break; // if the sender has no packet to send, DONE!
			if(total_acked==pktsNum) break;  // all packets have been sent, DONE!

			// STEP 2: if the window is not full, the packet is sent and the appropriate variables
			//	are updated and at timer is started if not done before
			// 	send packets
			int num = windowSize-send_not_acked ;
			int start = total_acked + send_not_acked;
			for(int i=start; i<start+num ;i++){
				if(i==pktsNum) break;
				byte[] sendData = packets[i].getUDPdata();
				DatagramPacket sendData_pkt = new DatagramPacket(sendData,sendData.length,address,portForData);
				sendSocket.send(sendData_pkt);
				send_not_acked = (send_not_acked+1) % seqMod;
				seqnumWriter.println(i);
			} 

			// STEP 3: waits for acks	
			while(1==1){
				acksocket.setSoTimeout(timeOut);
				try{
					byte[] receivedata = new byte[1024];
					DatagramPacket pkt = new DatagramPacket(receivedata,receivedata.length);
					acksocket.receive(pkt);
					packet receivedata_pkt = packet.parseUDPdata(receivedata);
					int receACK = receivedata_pkt.getSeqNum();
					ackWriter.println(receACK);
					if(receACK==expectACK){
						expectACK= (expectACK+1)%seqMod;
					}
					else {
						int lost = receACK-total_acked+1;
						send_not_acked = (send_not_acked-lost)%seqMod;
						total_acked = total_acked+lost;
					}					
				}
				 catch(SocketTimeoutException e){
					send_not_acked =0;
					break;
				}
			}
		} 

		// STEP 4: After all contents of the file have been transmitted successfully
		//	to the receiver, the sender should send an EOT pkt to the receiver.
		packet eot = packet.createEOT(pktsNum%seqMod);
		byte[] send = eot.getUDPdata();
		DatagramPacket sendeot = new DatagramPacket(send,send.length,address,portForData);
		sendSocket.send(sendeot);
		
		// wait for EOT ack, assuming EOT will not lost --> check is unnessary
		byte[] eto_rece_data = new byte[1024];
		DatagramPacket eotPacket = new DatagramPacket(eto_rece_data,eto_rece_data.length);
		acksocket.receive(eotPacket);
		packet rece_eot_ack =packet.parseUDPdata(eto_rece_data);
		if(rece_eot_ack.getSeqNum()==eot.getSeqNum()){
			seqnumWriter.close();
			ackWriter.close();
			acksocket.close();
			sendSocket.close();
		}	
	}
}



