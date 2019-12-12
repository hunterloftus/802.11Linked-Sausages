package wifi;

import java.util.Queue;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import java.util.Random;
import java.io.PrintWriter;

import rf.RF;
/**
 * 
 * @author Ana-Lea Nishiyama, Anna Watson, Hunter Loftus
 * @version 12.12.19
 * @apiNote This is our Sender Thread that grabs the packet from the LinkLayer and sends it!
 *
 */
public class Sender implements Runnable {
	private static final int QueueCap = 4; // Cap on how many jobs we can take
	private RF theRF;
	private int backOffWindow = theRF.aCWmin;
	private boolean ForceMaxBackOff = false;
	private Boolean[] ackQueue = new Boolean[4096];
	private ArrayBlockingQueue<Packet> sendQueue = new ArrayBlockingQueue<Packet>(QueueCap); // make this a packet
																								// object

	private short ourMAC; // Our MAC address
	short ControlByte;
	short DestAddress;
	short SourceAddress;
	int PacketLength;
	byte[] data;
	int backoffWindow;
	private final static int STARTING_TIMEOUT_WINDOW = 4000;
	int timeoutWindow = 4000; // 4 seconds seems to be the magic first number
	private Packet packet = null;
	private PrintWriter output; // GUI thing
	int reTransmit = 0;
	Random rand = new Random();
	long lastBeaconTimeStamp = 0;	

	
/**
 * Creates our Sender Thread
 * @param theRF the radio frequency our send and receive uses to broadcast packets
 * @param ackQueue a queue holding ACKS
 * @param sendQueue a queue holding packets to transmit
 * @param ourMAC our MAC address
 * @param output the GUI window output
 */
	public Sender(RF theRF, Boolean[] ackQueue, ArrayBlockingQueue<Packet> sendQueue, short ourMAC,
			PrintWriter output) {
		this.sendQueue = sendQueue;
		this.ourMAC = ourMAC;
		this.theRF = theRF;
		this.ackQueue = ackQueue;
		this.output = output;
	}
/**
 * 
 * DIFS waits DIFS timing then synch's clock to the nearest 50ms interval
 * 
 */
	private void DIFS() {
		if(LinkLayer.debugMode==-1){output.println("IDLE_DIFS_WAIT");}
		int wait = RF.aSIFSTime + (RF.aSlotTime + RF.aSlotTime); // DIFS = SIFS + (SlotTime x 2) by 802.11 standard
		try {
			Thread.sleep(wait); // wait DIFS amount of time
		}

		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		while(true) {
			if(theRF.clock()+LinkLayer.clockModifier%100!=0||theRF.clock()+LinkLayer.clockModifier%100!=50) { //synch clock to nearest 50ms interval
				System.out.println("Time after DIFS:" + theRF.clock()+LinkLayer.clockModifier);
				break;
			}
		}
	}

	/**
	 * WaitBackOffWindow waits the number of slots inputed
	 * @param backOff number of slot times we need to wait
	 */
	private void WaitBackoffWindow(int backOff) { // waits the exponential back off time
		while (backOff > 0) { // If our backoff is not done yet
			if (theRF.inUse() == true) { // if rf in use
				while (theRF.inUse() == true);
				DIFS();
			}
			try {
				Thread.sleep(theRF.aSlotTime); // start counting down back off
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			backOff = backOff - 1; // went through one iteration
		}
	}

	/**
	 * StartBackoff takes in the back off window size and will wait the necessary time before it will try to send a packet
	 * @param backoff size of our backOff window
	 */
	private void StartBackoff(int backoff) {
		boolean sent = false; //if we sent the packet or not
		if (LinkLayer.slotSelection == 0) { //if we wait the max window time or roll the dice
			backoff = rand.nextInt(backOffWindow);// roll dice on window Size 
		} else {
			backoff = backOffWindow; // wait Max Window Size
		}
		if(LinkLayer.debugMode==-1) {output.println("backOff Chosen: " + backoff);}
		while (sent == false) {
			WaitBackoffWindow(backoff); //backOff
			DIFS();
			if (theRF.inUse() == false) {
				sendPacket();
				sent = true; // we sent no longer need to be looping
			} else { //updating Back off Window
				if(backOffWindow >= theRF.aCWmax){
					backOffWindow = theRF.aCWmax;
				} else
					backOffWindow = backOffWindow * 2;// increase the back off Window
			}
		}
	}

	/**
	 * AwaitingAck checks the ACKQueue if we received an ACK for our message, If there is no ACK we try to send the message again till we reach retry limit
	 */
	private void AwaitingAck() { // if send fails the First time
		try {
			Thread.sleep(timeoutWindow); // wait for our timeout value
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		if (ackQueue[packet.getSequenceNum()] == true) { // if we got an ack for our packet were done
			if(LinkLayer.debugMode==-1){output.println("Received ACK");}

			backOffWindow = theRF.aCWmin;// reset windows
			timeoutWindow = STARTING_TIMEOUT_WINDOW;
			LinkLayer.status = LinkLayer.TX_DELIVERED;
			return;
		} else {
			if (reTransmit >= theRF.dot11RetryLimit) { // retransmit limit reached
				reTransmit = 0;
				backOffWindow = theRF.aCWmin;// reset windows
				timeoutWindow = STARTING_TIMEOUT_WINDOW;
				System.out.println("Retry Limit Reached");
				LinkLayer.status = LinkLayer.TX_FAILED;

				packet = null;
				return;
			}
			System.out.println("ReTransmit");
			timeoutWindow = timeoutWindow + 1000;// add 1 seconds before timeout
			reTransmit++;
			StartBackoff(backOffWindow); // start trying to send again with new timeout Time
		}
	}

	/**
	 * CreateBeacon creates the Beacon to send to all users listening
	 * @param currentTime our RF's current Time
	 */
	private void createBeacon(long currentTime) {
		
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(currentTime + LinkLayer.clockModifier); //putting our clock into byte[]
		byte[] byteCurrentTime = buffer.array();		//getting the array
		//generating the Beacon packet to send
		packet = new Packet((short) 0b010, (short) 0, LinkLayer.seqNumber, (short) -1, ourMAC, byteCurrentTime, byteCurrentTime.length); 
		LinkLayer.seqNumber++;

	}
	
	/**
	 * Sends the Packet and calls Awaiting ACK to see if our message got heard
	 */
	private void sendPacket() { // sends the packet
		if (packet != null) {
			theRF.transmit(packet.getPacket()); //RF shooting the packet into the void

			if (packet.desAddr != -1) { //if we sent it to a specific user make sure it was delivered
				if(LinkLayer.debugMode==-1){output.println("moving to AWAIT_PACKET ater broadcasting DATA");}

				AwaitingAck();
			}
		}
		packet = null; //reset packet
	}

	/**
	 * Run is our Send threads main loop where it will generate packets and go through the 802.11 spec back off
	 */
	public void run() {
		output.println("Sender Thread Reporting For Duty!!");
		while (true) {// keeps the thread moving

			
			//Generates the packet to be sent
			while (packet == null) {
				long modifiedTime = theRF.clock() + LinkLayer.sendModifier;//clock syncing  
				if (LinkLayer.beaconInterval != -1) { //if beacons are turned on

					if (lastBeaconTimeStamp + (LinkLayer.beaconInterval * 1000) <= modifiedTime) { //if its time to send a beacon
						createBeacon(modifiedTime);
						lastBeaconTimeStamp = modifiedTime; //update the last time we sent a Beacon
					}
				}
				if (sendQueue.peek() != null) {
					packet = sendQueue.poll(); //grab a packet form the send queue
					System.out.println("Send Packet");
				}
								
				//sending the Packet
			}
			if (theRF.inUse() == false) { // if there is no transmission you have the clear to transmit
				DIFS();
				if (theRF.inUse() == false) { // RF not in use
					sendPacket();
				} else {
					if(LinkLayer.debugMode==-1) {output.println("backOffWindow: " + backOffWindow);}
					StartBackoff(backOffWindow); //RF was in use so we start backing off
				}
			} else {
				StartBackoff(backOffWindow); // RF was in use so we start backing off
			}			
		}
	}
}