 package wifi;

 

 
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.print.attribute.standard.OutputDeviceAssigned;

import rf.RF;

public class Receiver implements Runnable{
	
	public Queue<Packet> DataQueue = new ArrayBlockingQueue<Packet>(1000);
	//this will end up being a packet object
	
	private RF theRF;
	private Queue<Boolean> ackQueue = new ArrayBlockingQueue<Boolean>(1000);
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
	
	//receiver loop
	
	//send section for ACKS//AWKS

	public  void run() {
		System.out.println("Receiver Thread Reporting For Duty!!");

		while(true) { //loop to keep thread alive
			byte[] IncomingByteArray = theRF.receive();
			//Packet packet = new Packet();
			Packet IncomingPacket = null;
			
			/*
			short[] ControlBits = Packet.getControlBits(IncomingByteArray);
			
			frameType = ControlBits[0];
			retry = ControlBits[1];
			seqNumber = ControlBits[2];
			DestAddress = ControlBits[3];
			SourceAddress = ControlBits[4];
			PacketLength = IncomingByteArray.length;
			*/
			
			IncomingPacket = new Packet(IncomingByteArray);
			
			//IncomingPacket = new Packet(frameType, retry, seqNumber, DestAddress, SourceAddress, IncomingByteArray, PacketLength);
			
			
			
			
			

			DataQueue.add(IncomingPacket); //put packet into queue
		}
		
	}
	
}
