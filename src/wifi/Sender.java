 package wifi;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

public class Sender implements Runnable{
	
	private RF theRF;
	private Boolean[] ackQueue = new Boolean[4096];
	private Queue<Packet> sendQueue = new ArrayBlockingQueue<Packet>(1000); //make this a packet object

	private short ourMAC;       // Our MAC address
	short ControlByte;
	short DestAddress;
	short SourceAddress;
	int PacketLength;
	byte[] data;
	int backoffWindow;
	int timeoutWindow = 20000;
	private Packet packet;


	
	//go through the decision tree
	
	public Sender(RF theRF, Boolean[] ackQueue, Queue<Packet> sendQueue, short ourMAC) {
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
    
	private void emptyWait(int backOff) { //waits the exponential back off time
		
		
		while(backOff>0) {
			if(theRF.inUse()==true) {
				while(theRF.inUse()==true);
				DIFS();
			}
	        try{
	            Thread.sleep(theRF.aSlotTime); //start counting down back off
	        }
	        catch(InterruptedException ex){
	            Thread.currentThread().interrupt();
	        }
	        backOff = backOff-1; //need to roll dice with new value
		}
	}
	
    private void backOff(int backoff) { //
    	boolean sent = false;
    	while(sent==false) {
    		emptyWait(backoff);
    		DIFS();
    		if(theRF.inUse()==false) {
    			sendPacket();
    			sent = true;
    		}
    		else {
    			//System.out.println("I waited: " + backoff);

    			backoff = backoff * backoff;
    		}
    	}
    }
    
    
    private void reTransmit() {
    	try{
            Thread.sleep(timeoutWindow); //start counting down back off
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
		System.out.println(packet.getSequenceNum());
		System.out.println(ackQueue[packet.getSequenceNum()]);
    	if(ackQueue[packet.getSequenceNum()]==true) {
    		return;
    	}
    	else {
    		System.out.println("retransmitting");
    		backOff(2);
    	}
    	//get seqNumber
    	
    }
    
    
    private void sendPacket() {
        	System.out.println(packet.toString());
    		theRF.transmit(packet.getPacket());
    		reTransmit();
    }
	
	
	public void run() {
		System.out.println("Sender Thread Reporting For Duty!!");
		while(true) {//keeps the thread moving
			
			 if(sendQueue.isEmpty()==true) {
				 DIFS(); //wait some time as not to crowd cpu
			 }
			 else {
				 packet = sendQueue.poll();
				 if(theRF.inUse()==false){ //if there is no transmission you have the clear to transmit
					 DIFS();
					 if(theRF.inUse()==false) {
						 sendPacket();
					 }
					 else {
						 backOff(2);
					 }
					 
				 }
				 else {
					backOff(2);
				 }          
		     }	 
		}		
	}//end of run
}
