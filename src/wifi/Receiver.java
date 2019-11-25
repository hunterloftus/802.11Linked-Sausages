 package wifi;

 

 
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.print.attribute.standard.OutputDeviceAssigned;

import rf.RF;

public class Receiver implements Runnable{
	
	public Queue<Packet> DataQueue = new ArrayBlockingQueue<Packet>(1000);
	//this will end up being a packet object
	
	private RF theRF;
	private Queue<Boolean> ackQueue = new ArrayBlockingQueue<Boolean>(4096);
	private short ourMAC;       // Our MAC address
	
	short ControlByte;
	short DestAddress;
	short SourceAddress;
	short frameType;
	short retry;
	short seqNumber;
	
	int PacketLength;

	
	
	
	private void SIFS() {
        try{
            Thread.sleep(RF.aSIFSTime); //wait SIFS amount of time
        }
        
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
	}
	
	
	public Receiver(RF theRF, Queue<Boolean> ackQueue, Queue<Packet> dataQueue, short ourMAC) {
		this.DataQueue = dataQueue;
		this.ourMAC = ourMAC;
		this.theRF = theRF;
		this.ackQueue = ackQueue;
	}
	
	private void sendACK(Packet packet) {
		//get seqNumber
		packet.setACK();
		
		System.out.println("ACK:" + packet.toString());
		
		
		SIFS();
		theRF.transmit(packet.getPacket());
		
		
	}
	
	
	private int toUs(byte[] packet) {
		int dstAddr= packet[2];
		dstAddr = dstAddr<<8;
		dstAddr = dstAddr|packet[3];
		return dstAddr;
		
		
	}
	
	//receiver loop
	
	//send section for ACKS//AWKS

	public  void run() {
		System.out.println("Receiver Thread Reporting For Duty!!");

		while(true) { //loop to keep thread alive
			if(theRF.dataWaiting()==true) {
				byte[] IncomingByteArray = theRF.receive();
				//Packet packet = new Packet();
				Packet IncomingPacket = null;
				System.out.println("inhere");
				
				
				
				if(toUs(IncomingByteArray)==ourMAC){
					IncomingPacket = new Packet(IncomingByteArray);
					sendACK(IncomingPacket);
					
					//IncomingPacket = new Packet(frameType, retry, seqNumber, DestAddress, SourceAddress, IncomingByteArray, PacketLength);
					
					DataQueue.add(IncomingPacket); //put packet into queue
				}
				else {
					System.out.println("notforus");
				}
			}	
			
		}
		
	}
	
}
