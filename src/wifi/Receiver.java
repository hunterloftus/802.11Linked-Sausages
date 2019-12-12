package wifi;

 

 
import java.util.HashMap;
import java.util.Queue;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import javax.print.attribute.standard.OutputDeviceAssigned;

import rf.RF;

public class Receiver implements Runnable{
	private ArrayBlockingQueue<Packet> DataQueue = new ArrayBlockingQueue<Packet>(1000);
	//this will end up being a packet object
	public static HashMap<Short, Short> seqNum;
	
	private RF theRF;
	private Boolean[] ackQueue = new Boolean[4096];
	private short ourMAC;       // Our MAC address
	
	short ControlByte;
	short DestAddress;
	private PrintWriter output; //GUI Thing
	short SourceAddress;
	short frameType;
	short retry;
	short seqNumber;
    final short ACK = 0b001;

	
	int PacketLength;

	
	
	
	private void SIFS() {
        try{
            Thread.sleep(RF.aSIFSTime); //wait SIFS amount of time
        }
        
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
	}
	
	
	public Receiver(RF theRF, Boolean[] ackQueue, ArrayBlockingQueue<Packet> dataQueue, short ourMAC, PrintWriter output) {
		this.DataQueue = dataQueue;
		this.ourMAC = ourMAC;
		this.theRF = theRF;
		this.ackQueue = ackQueue;
		this.output = output;
		
		seqNum = new HashMap<>();
	}
	
	private void sendACK(Packet packet) { //sends an ack back to whoever send the packet to us
		packet.setACK(); //change control bits
		packet.setDesAddr(packet.scrAddr);
		packet.setScrAddr(ourMAC);
		//SIFS(); we should do this but...
		System.out.println("Sending ACK");
		theRF.transmit(packet.getPacket());
	}
	
	
	
	private int toUs(byte[] packet) { //sees if the packet was ment for us
		return (short) ((packet[2] << 8) | (packet[3] & 0xFF));
		
	}
	
	
	
	

	
	
	
	private boolean checkSeqNum(Packet p) {
		boolean ordered = false;
		int val = 0;
		if(seqNum.containsKey(p.getScrAddr())) { // If MAC address already exists then check if the their sequence
			//number is sequential to the their MAC address.
			val = seqNum.get(p.getScrAddr());
			if ((val + 1) == p.getSequenceNum()) {
				ordered = true;
			}
			else {
				//if its not sequential
				ordered = false;
			}
			//set the new sequence number	
		}
		else {
			ordered = true;
		}
		//Get value, 
		//check in  in
		//seqNum.put(key, value)
		seqNum.put(p.getScrAddr(), (short) p.getSequenceNum());
		
		return ordered;
		
	}
	
	//receiver loop
	
	//send section for ACKS//AWKS

	public  void run() {
		System.out.println("Receiver Thread Reporting For Duty!!");
		while(true) { //loop to keep thread alive
		//	if(theRF.dataWaiting()) { //checks if data is waiting.
				byte[] IncomingByteArray = theRF.receive();
				//Packet packet = new Packet();
				Packet IncomingPacket = new Packet(IncomingByteArray);
				
				if(seqNum.containsKey(IncomingPacket.getScrAddr())) {
					short lastSeqNum = seqNum.get(IncomingPacket.getScrAddr()); //returns seqNumber
					if(IncomingPacket.getSequenceNum()==lastSeqNum) {	
					}
				}
				if(toUs(IncomingByteArray)==ourMAC || toUs(IncomingByteArray)==-1){ //if the packet was meant for us
					if(IncomingPacket.getFrameType()==ACK) { //if packet received was an ack
						ackQueue[IncomingPacket.getSequenceNum()] = true; //tell sender we received an ack
					}
					else if(toUs(IncomingByteArray)==ourMAC){ //if the message was meant for specifically us
						sendACK(IncomingPacket); //send ACK
						DataQueue.add(IncomingPacket); //put packet into queue
						
						if(checkSeqNum(IncomingPacket) == false) {
							output.print("THis is not ordered");
							System.out.println("Here");
						}
						else {
							output.print("Yay, I'm ordered! :D");
							System.out.println("Here1");
						}
						
						
						//Check if the their MAC address was in the map
					}
					else if(toUs(IncomingByteArray)==-1) { //if it was an open message to all who will listen
						if(IncomingPacket.getFrameType()==0b010) { //if its a beacon
							System.out.println("Received Beaon");
							long ourTime = LinkLayer.clockModifier + theRF.clock();
							ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
							buffer.put(IncomingPacket.getData());
							long theirTime = buffer.getLong();
							LinkLayer.updateClock(ourTime, theirTime);
						}
						else {
							DataQueue.add(IncomingPacket); //put packet into queue
						}
					}
				}
			//}				
		}
	}
}