 package wifi;

 

 
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.print.attribute.standard.OutputDeviceAssigned;

import rf.RF;

public class Receiver implements Runnable{
	
	public Queue<Packet> DataQueue = new ArrayBlockingQueue<Packet>(1000);
	//this will end up being a packet object
	
	private RF theRF;
	private Boolean[] ackQueue = new Boolean[4096];
	private short ourMAC;       // Our MAC address
	
	short ControlByte;
	short DestAddress;
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
	
	
	public Receiver(RF theRF, Boolean[] ackQueue, Queue<Packet> dataQueue, short ourMAC) {
		this.DataQueue = dataQueue;
		this.ourMAC = ourMAC;
		this.theRF = theRF;
		this.ackQueue = ackQueue;
	}
	
	private void sendACK(Packet packet) {
		packet.setACK(); //change control bits
		//SIFS(); we should do this but...
		System.out.println("Sending ACK");
		theRF.transmit(packet.getPacket());
		
		
	}
	
	
	
	private int toUs(byte[] packet) {
		return (short) ((packet[2] << 8) | (packet[3] & 0xFF));
		
	}
	
	//receiver loop
	
	//send section for ACKS//AWKS

	public  void run() {
		System.out.println("Receiver Thread Reporting For Duty!!");

		while(true) { //loop to keep thread alive
			if(theRF.dataWaiting()) {
				byte[] IncomingByteArray = theRF.receive();
				//Packet packet = new Packet();
				Packet IncomingPacket;
							
				if(toUs(IncomingByteArray)==ourMAC || toUs(IncomingByteArray)==-1){
					
					IncomingPacket = new Packet(IncomingByteArray);
					if(toUs(IncomingByteArray)==-1 && IncomingPacket.getFrameType()!=ACK){
						sendACK(IncomingPacket);
						System.out.println("isPacket");
						DataQueue.add(IncomingPacket); //put packet into queue

					}				
					if(IncomingPacket.getFrameType()==ACK) {
						
						System.out.println("is ACK");
						ackQueue[IncomingPacket.getSequenceNum()] = true;
					}
					if(toUs(IncomingByteArray)==-1) {
						DataQueue.add(IncomingPacket); //put packet into queue
					}

				}
				else {
					//System.out.println("notforus");
				}
			}	
			
		}
		
	}
	
}
