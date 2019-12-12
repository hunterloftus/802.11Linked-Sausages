package wifi;

 

 
import java.util.HashMap;
import java.util.Queue;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import javax.print.attribute.standard.OutputDeviceAssigned;

import rf.RF;
/**
 * 
 * @author Ana-Lea Nishiyama, Anna Watson, Hunter Loftus
 * @verson 12.12.2019
 * @apiNote This is out Receiver Thread, it pulls packets from the RF and deciphers them
 *
 */
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

	
	
	/**
	 * Waits Sifs
	 */
	private void SIFS() {
        try{
            Thread.sleep(RF.aSIFSTime); //wait SIFS amount of time
        }
        
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
	}
	
	/**
	 * Constructs the Receiver Thread
	 * @param theRF the RF we use to receive
	 * @param ackQueue Where we store ACKS
	 * @param dataQueue where we put data we receive
	 * @param ourMAC our MAC address
	 * @param output the GUI printer
	 */
	public Receiver(RF theRF, Boolean[] ackQueue, ArrayBlockingQueue<Packet> dataQueue, short ourMAC, PrintWriter output) {
		this.DataQueue = dataQueue;
		this.ourMAC = ourMAC;
		this.theRF = theRF;
		this.ackQueue = ackQueue;
		this.output = output;
		
		seqNum = new HashMap<>();
	}
	/**
	 * This sends an ACK
	 * @param packet the packet we received
	 */
	private void sendACK(Packet packet) { //sends an ack back to whoever send the packet to us
		packet.setACK(); //change control bits
		packet.setDesAddr(packet.scrAddr);
		packet.setScrAddr(ourMAC);
		//SIFS(); we should do this but...
		System.out.println("Sending ACK");
		theRF.transmit(packet.getPacket());
	}
	
	
	/*
	 * if the packet received was meant for us
	 */
	private int toUs(byte[] packet) { //sees if the packet was meant for us
		return (short) ((packet[2] << 8) | (packet[3] & 0xFF)); //checks DST address field
		
	}
	
	/*
	 * checks the Sequence Numbers
	 */
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
				ordered = false;
			}
		}
		else {
			ordered = true;
		}

		seqNum.put(p.getScrAddr(), (short) p.getSequenceNum());
		
		return ordered;
		
	}
		
	/**
	 * Our main Thread Loop that pulls messages from the RF and places them where they need to go
	 */
	public  void run() {
		System.out.println("Receiver Thread Reporting For Duty!!");
		while(true) { //loop to keep thread alive
				byte[] IncomingByteArray = theRF.receive(); //blocks till there is a packet
				//Packet packet = new Packet();
				Packet IncomingPacket = new Packet(IncomingByteArray);
				
				if(seqNum.containsKey(IncomingPacket.getScrAddr())) {
					short lastSeqNum = seqNum.get(IncomingPacket.getScrAddr()); //returns seqNumber
					if(IncomingPacket.getSequenceNum()==lastSeqNum) {	
					}
				}
				if((toUs(IncomingByteArray)==ourMAC || toUs(IncomingByteArray)==-1) && IncomingPacket.checkCRC()){ //if the packet was meant for us
					if(IncomingPacket.getFrameType()==ACK) { //if packet received was an ack
						ackQueue[IncomingPacket.getSequenceNum()] = true; //tell sender we received an ack
					}
					
					else if(toUs(IncomingByteArray)==ourMAC){ //if the message was meant for specifically us
						sendACK(IncomingPacket); //send ACK
						DataQueue.add(IncomingPacket); //put packet into queue
						
						if(checkSeqNum(IncomingPacket) == false) {
							if(LinkLayer.debugMode==-1) {output.print("Sequence Number Order Broken");}
						}
					}
					else if(toUs(IncomingByteArray)==-1) { //if it was an open message to all who will listen
						if(IncomingPacket.getFrameType()==0b010) { //if its a beacon
							
							byte[] data = new byte[8];
							long ourTime = LinkLayer.clockModifier + theRF.clock();
							
							System.out.println("InPack: "+IncomingPacket);
							data = IncomingPacket.getData();							
							long theirTime = 0;
							for (int i = 0; i < data.length; i++) //pull long from Beacon Packet
							{
								theirTime = (theirTime << 8) + (data[i] & 0xff);
							}
							LinkLayer.updateClock(ourTime, theirTime);
						}
						else {
							DataQueue.add(IncomingPacket); //put packet into queue
						}
					}
				}
				else {
					if((toUs(IncomingByteArray)==ourMAC || toUs(IncomingByteArray)==-1) && !(IncomingPacket.checkCRC())) {
						System.out.println("We encontered a bad CRC!");
					}
				}
		}
	}
}