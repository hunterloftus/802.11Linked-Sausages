 package wifi;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Random;

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
	int reTransmit = 0;
	Random rand = new Random();



	
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
		
		
		while(backOff>0) { //If our backof isnt done yet
			if(theRF.inUse()==true) { //if rf in use
				while(theRF.inUse()==true);
				DIFS();
			}
	        try{
	            Thread.sleep(theRF.aSlotTime); //start counting down back off
	        }
	        catch(InterruptedException ex){
	            Thread.currentThread().interrupt();
	        }
	        backOff = backOff-1; //went through one interaction
		}
	}
	
    private void backOff(int backoff) { //handles the waiting till transmission is open
    	boolean sent = false;
    	while(sent==false) {
    		emptyWait(backoff);
    		DIFS();
    		if(theRF.inUse()==false) {
    			sendPacket();
    			sent = true; //we sent no longer need to be looping
    		}
    		else {
    			backoff = backoff * backoff; //Exponential backoff, need to fix to use frame size and not the number
    		}
    	}
    }
    
    
    private void reTransmit() { //if send fails the irst time
    	try{
            Thread.sleep(timeoutWindow); //wait for our timeout value
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
    	if(ackQueue[packet.getSequenceNum()]==true) { //if we got an ack for our packet were done
    		return;
    	}
    	else {
    		if(reTransmit>=theRF.dot11RetryLimit) { //retransmit limit reached
    			reTransmit=0;
    			System.out.println("Retry Limit Reached");
    			return;
    		}
    		//System.out.println("retransmitting");

    		reTransmit++;
    		backOff(rand.nextInt(theRF.aCWmin)); //start trying to send again
    	}
    	//get seqNumber
    	
    }
    
    
    private void sendPacket() { //sends the packet
    	
    		theRF.transmit(packet.getPacket());
    		if(packet.desAddr!=-1) {
        		reTransmit();
    		}
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
					 if(theRF.inUse()==false) { //still not in use
						 sendPacket();
					 }
					 else {
						 backOff(rand.nextInt(theRF.aCWmin)); //was in use so we start backing off
					 } 
				 }
				 else {
					backOff(rand.nextInt(theRF.aCWmin)); //was in use so we start backing off
				 }          
		     }	 
		}		
	}//end of run
}
