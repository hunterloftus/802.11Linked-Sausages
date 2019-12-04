 package wifi; 
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

/**
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @author richards
 */
public class LinkLayer implements Dot11Interface, Runnable
{
	private RF theRF;           // You'll need one of these eventually
	private short ourMAC;       // Our MAC address
	private PrintWriter output; // The output stream we'll write to
	private static final int QueueCap = 4; //Cap on how many jobs we can take

	private Boolean[] ackQueue = new Boolean[4096];
	private ArrayBlockingQueue<Packet> sendQueue = new ArrayBlockingQueue<Packet>(QueueCap); //make this a packet object
	public static ArrayBlockingQueue<Packet> DataQueue = new ArrayBlockingQueue<Packet>(1000);
	short seqNumber;





	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output will
	 * be written.
	 * @param ourMAC  MAC address
	 * @param output  Output stream associated with GUI

 */
	
	public LinkLayer(short ourMAC, PrintWriter output) {
		this.ourMAC = ourMAC;
		this.output = output;      
		theRF = new RF(null, null);
		output.println("LinkLayer: Constructor ran.");
		//shoot of sender and receiver here
		
		for(int i=0;i<4096;i++) {
			ackQueue[i]=false;
		}
		
		
		Receiver recv = new Receiver(theRF, ackQueue, DataQueue, ourMAC, output);
		Sender sender = new Sender(theRF, ackQueue, sendQueue, ourMAC, output);
		(new Thread(recv)).start();
		(new Thread(sender)).start();
		seqNumber = 0;
	}
	
	
	

		/*
	 	public LinkLayer(short ourMAC, PrintWriter output) {
		this.ourMAC = ourMAC;
		this.output = output;      
		theRF = new RF(null, null);
		output.println("LinkLayer: Constructor ran.");
	}
	*/
	 
	 

	/**
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 */
	public int send(short dest, byte[] data, int len) {
		if(sendQueue.size()>=QueueCap) {		
			output.println("Dropping Job Queue full");
		}
		short frameType = 000;
		short retry = 0;
		
		Packet packet = new Packet(frameType, retry, seqNumber, dest, ourMAC, data, len);
		output.println("I sent: " + len + " Bytes of Data to " + packet.getDesAddr());
		sendQueue.add(packet);
		output.println(packet.toString());
		
		seqNumber++;
		if(seqNumber>=4098) {
			seqNumber=0;
		}
		return len;
		
		
	}

	/**
	 * Recv method blocks until data arrives, then writes it an address info into
	 * the Transmission object.  See docs for full description.
	 */
	public int recv(Transmission t) {
		//output.println("LinkLayer:blocking on recv()");
		Packet packet;
		
		while(DataQueue.isEmpty()) {
	        try{
	            Thread.sleep(5); //wait some time to not flood cpu
	            }
	        
	        catch(InterruptedException ex){
	            Thread.currentThread().interrupt();
	        }
		}
		try { 
			packet = DataQueue.take();
			//short[] ControlBits = Packet.getControlBits(packet.packet);
			
			
			//output.println("I Received " + packet.packet.length + "Bytes from " + ControlBits[4]);
			t.setBuf(packet.packet);
			t.setDestAddr(packet.desAddr);
			t.setSourceAddr(packet.scrAddr);

			output.println("I Received a Packet with " + packet.getPacket().length + " Bytes from " + packet.getScrAddr());
			output.println(packet.niceToString());
			output.println("Sending ACK");
		} catch (InterruptedException e) {
			output.println("Recv is having a bad day");
			return -1;
		}
			output.println("Received " + packet.data.length + " of Data");
			return packet.data.length; // size of packet received
	}

	/**
	 * Returns a current status code.  See docs for full description.
	 */
	public int status() {
		output.println("LinkLayer: Faking a status() return value of 0");
		return 0;
	}

    private void DIFS(){
        int wait = RF.aSIFSTime + (RF.aSlotTime + RF.aSlotTime); //DIFS = SIFS + (SlotTime x 2) by 802.11 standard
        try{
            Thread.sleep(wait); //wait DIFS amount of time
        }
        
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }

    }
	
	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmd, int val) {
		output.println("LinkLayer: Sending command "+cmd+" with value "+val);
		return 0;
	}



	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
