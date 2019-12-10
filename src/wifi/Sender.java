package wifi;

import java.util.Queue;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import java.util.Random;
import java.io.PrintWriter;

import rf.RF;

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

	public Sender(RF theRF, Boolean[] ackQueue, ArrayBlockingQueue<Packet> sendQueue, short ourMAC,
			PrintWriter output) {
		this.sendQueue = sendQueue;
		this.ourMAC = ourMAC;
		this.theRF = theRF;
		this.ackQueue = ackQueue;
		this.output = output;
	}

	private void DIFS() {
		int wait = RF.aSIFSTime + (RF.aSlotTime + RF.aSlotTime); // DIFS = SIFS + (SlotTime x 2) by 802.11 standard
		try {
			Thread.sleep(wait); // wait DIFS amount of time
		}

		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

	}

	private void wait(int wait) {
		try {
			Thread.sleep(wait); // wait DIFS amount of time
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private void WaitBackoffWindow(int backOff) { // waits the exponential back off time
		while (backOff > 0) { // If our backoff is not done yet
			if (theRF.inUse() == true) { // if rf in use
				while (theRF.inUse() == true)
					;
				DIFS();
			}
			try {
				Thread.sleep(theRF.aSlotTime); // start counting down back off
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			backOff = backOff - 1; // went through one interaction
		}
	}

	private void StartBackoff(int backoff) { // handles the waiting till transmission is open
		boolean sent = false;
		if (LinkLayer.slotSelection == 0) {
			backoff = rand.nextInt(backOffWindow);// wait max time each
		} else {
			backoff = backOffWindow; // wait Max Back off time
		}
		while (sent == false) {
			WaitBackoffWindow(backoff);
			DIFS();
			if (theRF.inUse() == false) {
				sendPacket();
				sent = true; // we sent no longer need to be looping
			} else {
				if (backOffWindow >= theRF.aCWmax) {
					backOffWindow = theRF.aCWmax;
				} else
					backOffWindow = backOffWindow * 2;// increase the back off Window
			}
		}
	}

	private void AwaitingAck() { // if send fails the First time
		try {
			Thread.sleep(timeoutWindow); // wait for our timeout value
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		if (ackQueue[packet.getSequenceNum()] == true) { // if we got an ack for our packet were done
			backOffWindow = theRF.aCWmin;// reset windows
			timeoutWindow = STARTING_TIMEOUT_WINDOW;
			return;
		} else {
			if (reTransmit >= theRF.dot11RetryLimit) { // retransmit limit reached
				reTransmit = 0;
				System.out.println("Retry Limit Reached");
				packet=null;
				return;
			}
			System.out.println("ReTransmit");
			timeoutWindow = timeoutWindow + 1000;// add 1 seconds before timeout
			reTransmit++;
			StartBackoff(backOffWindow); // start trying to send again
		}
		// get seqNumber
	}

	private void createBeacon(long currentTime) {

		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(currentTime);
		byte[] byteCurrentTime = buffer.array();
		
		
		Packet localPacket = new Packet((short) 0b010, (short) 0, LinkLayer.seqNumber, (short) -1, ourMAC, byteCurrentTime, byteCurrentTime.length);
		packet = localPacket;
		LinkLayer.seqNumber++;
		System.out.println("Created Beacon");

	}

	private void sendPacket() { // sends the packet
		if(packet != null) {
			theRF.transmit(packet.getPacket());
		
		
		if (packet.desAddr != -1) {
			AwaitingAck();
		}
		}
		packet=null;
	}

	long lastBeaconTimeStamp = 0;
	boolean BeaconsTurn = false;

	public void run() {
		System.out.println("Sender Thread Reporting For Duty!!");
		while (true) {// keeps the thread moving

			//if (sendQueue.isEmpty() == true) {
				//DIFS(); // wait some time as not to crowd cpu
			//}
			
			
			while(packet==null) {
				long modifiedTime = theRF.clock() + LinkLayer.clockModifier;
				
				if (lastBeaconTimeStamp + (LinkLayer.beaconInterval * 1000) <= modifiedTime) {
					createBeacon(modifiedTime);
					lastBeaconTimeStamp = modifiedTime;
				}
				if(sendQueue.peek()!=null) {
					packet = sendQueue.poll();
					System.out.println("Send Packet");
				}	
			}
			
			
			
//			if (LinkLayer.beaconInterval != -1) { // Beacon Path
//				if (lastBeaconTimeStamp + (LinkLayer.beaconInterval * 1000) <= modifiedTime) { // we have to send a
//																								// beacon
//					BeaconsTurn = true;
//					System.out.println("Last BTS: " + lastBeaconTimeStamp);
//					lastBeaconTimeStamp = modifiedTime;
//					System.out.println("New BTS: " + lastBeaconTimeStamp);
//					System.out.println("Modified Time: " + modifiedTime);
//				}
//			}
//			try {
//				if (!BeaconsTurn) {
//				}
//			} catch (InterruptedException e) {
//				output.println("Send Queue Error");
//			}
					
			if (theRF.inUse() == false) { // if there is no transmission you have the clear to transmit
				DIFS();
				if (theRF.inUse() == false) { // ll not in use
					sendPacket();
				} else {
					StartBackoff(backOffWindow); // was in use so we start backing off
				}
			} else {
				StartBackoff(backOffWindow); // was in use so we start backing off
			}
		}
		// }
	} // end of run
}
