package wifi;

 
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

public class Sender implements Runnable{

	
	private RF theRF;
	private Queue<Boolean> ackQueue = new ArrayBlockingQueue<Boolean>(1000); //queue to pass ACKS back and forth
	private Queue<Packet> sendQueue = new ArrayBlockingQueue<Packet>(1000); //make this a packet object

	private short ourMAC;       // Our MAC address
	short ControlByte;
	short DestAddress;
	short SourceAddress;
	int PacketLength;
	byte[] data;


	
	//go through the decision tree
	
	public Sender(RF theRF, Queue<Boolean> ackQueue, Queue<Packet> sendQueue, short ourMAC) {
		this.sendQueue = sendQueue;
		this.ourMAC = ourMAC;
		this.theRF = theRF;
		this.ackQueue = ackQueue;
		
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
	
	
	public  void run() {
		System.out.println("Sender Thread Reporting For Duty!!");
		while(true) {//keeps the thread moving
			if(theRF.inUse()==false){ //if there is no transmission you have the clear to transmit
                if(sendQueue.isEmpty()==true) {
                	
                }
                else {
    				Packet PackettoSend = sendQueue.poll();
    				theRF.transmit(PackettoSend.returnPacket());

                }
            }
			else {
				DIFS(); //Wait DIFS as not to waste CPU time
			}
			
		}
		
	}
}
