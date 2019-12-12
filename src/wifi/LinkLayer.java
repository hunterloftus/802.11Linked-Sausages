 package wifi; 
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
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
	public static final int QueueCap = 4; //Cap on how many jobs we can take

	private Boolean[] ackQueue = new Boolean[4096];
	private ArrayBlockingQueue<Packet> sendQueue = new ArrayBlockingQueue<Packet>(QueueCap); //make this a packet object
	public static ArrayBlockingQueue<Packet> DataQueue = new ArrayBlockingQueue<Packet>(QueueCap);
	public static short seqNumber;
	//command variables
    public static int debugMode = -1;//0 for nothing, -1 for debug mode.
    public static int slotSelection = 0; //0 for random slot time, any number for Max window size.
    public static int beaconInterval = 20; //-1 for no beacons, any number for interval timing.
    public static long clockModifier = 0; //the difference between our clock and everyone else's.
    public static long sendModifier = 2298;
    public static final int SUCCESS = 1, UNSPECIFIED_ERROR = 2, RF_INIT_FAILED = 3, INSUFFICIENT_BUFFER_SPACE = 10;
    public static final int TX_DELIVERED = 4, TX_FAILED = 5, BAD_ADDRESS = 7, ILLEGAL_ARGUMENT = 9;
    public static int status;
    
    
	/**
	 * Constructor takes a MAC address and the PrintWriter to which our output will
	 * be written.
	 * @param ourMAC  MAC address
	 * @param output  Output stream associated with GUI
 */
	public LinkLayer(short ourMAC, PrintWriter output) {
		if(ourMAC>65536||ourMAC<1) {
			status = BAD_ADDRESS;
		}
		this.ourMAC = ourMAC;
		this.output = output;     
		try {
			theRF = new RF(null, null);
		}
		catch(Exception e){
			status = RF_INIT_FAILED;
		}
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
		status = SUCCESS;
	}
	
	/**
	 * Send method takes a destination, a buffer (array) of data, and the number
	 * of bytes to send.  See docs for full description.
	 */
	public int send(short dest, byte[] data, int len) {
		if(sendQueue.size()>=QueueCap) {	
			status = INSUFFICIENT_BUFFER_SPACE;
			output.println("Dropping Job, Queue full");
			return 0;
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
		if(t==null) {
			status = ILLEGAL_ARGUMENT;
		}
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
			t.setBuf(packet.getData());
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
		output.println("status: " + status);
		return status;
	}
	
	public static void updateClock(long ourTime, long theirTime) {
		if(theirTime>ourTime) {
			long timeDif = theirTime-ourTime;
			clockModifier += timeDif;
			System.out.println("Syncing Time, New Modifier is: " + clockModifier);
		}
		else {
			System.out.println("ourTime > theirTime, So we're not syncing the clock.");
			System.out.println("Our Time: " + ourTime);
			System.out.println("their Time: " + theirTime);
		}
		
	}

    private void DIFS(){
        int wait = RF.aSIFSTime + (RF.aSlotTime + RF.aSlotTime); //DIFS = SIFS + (SlotTime x 2) by 802.11 standard
        try{
            Thread.sleep(wait); //wait DIFS amount of time
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
		while(true) {
			if(theRF.clock()+LinkLayer.clockModifier%100!=0||theRF.clock()+LinkLayer.clockModifier%100!=50) {
				System.out.println("Time after DIFS:" + theRF.clock()+LinkLayer.clockModifier);
				break;
			}
		}
    }
	
	/**
	 * Passes command info to your link layer.  See docs for full description.
	 */
	public int command(int cmd, int val) {
		if(cmd==1) {
			if(val==0||val==-1) {
				debugMode=val;
				return 0;
			}
			output.print("0 or -1 are the only acceptable answers");
		}
		if(cmd==2) {
			slotSelection=val;
		}
		if(cmd==3) {
			beaconInterval = val;
		}
		if(debugMode==-1) {output.println("LinkLayer: Sending command "+cmd+" with value "+val);}
		
		return 0;
	}
	
	/**
	 * 
	 * 
	 */
	public int getDebug( ) {
		return debugMode;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}