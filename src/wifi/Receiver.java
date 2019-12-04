 package wifi;

 

 
import java.util.Queue;
import java.io.PrintWriter;

import java.util.concurrent.ArrayBlockingQueue;

import javax.print.attribute.standard.OutputDeviceAssigned;

import rf.RF;

public class Receiver implements Runnable{
	private ArrayBlockingQueue<Packet> DataQueue = new ArrayBlockingQueue<Packet>(1000);
	//this will end up being a packet object
	
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
	
	//receiver loop
	
	//send section for ACKS//AWKS

	public  void run() {
		System.out.println("Receiver Thread Reporting For Duty!!");

		while(true) { //loop to keep thread alive
			if(theRF.dataWaiting()) { //checks if data is waiting.
				byte[] IncomingByteArray = theRF.receive();
				//Packet packet = new Packet();
				Packet IncomingPacket = new Packet(IncomingByteArray);
				
				if(toUs(IncomingByteArray)==ourMAC || toUs(IncomingByteArray)==-1){ //if the packet was meant for us
					if(IncomingPacket.getFrameType()==ACK) { //if packet received was an ack
						ackQueue[IncomingPacket.getSequenceNum()] = true; //tell sender we received an ack
					}
					else if(toUs(IncomingByteArray)==ourMAC){ //if the message was meant for specifically us
						sendACK(IncomingPacket); //send ACK
						DataQueue.add(IncomingPacket); //put packet into queue

					}
					else if(toUs(IncomingByteArray)==-1) { //if it was an open message to all who will listen
						DataQueue.add(IncomingPacket); //put packet into queue
					}
				}
			}				
		}
	}
}
