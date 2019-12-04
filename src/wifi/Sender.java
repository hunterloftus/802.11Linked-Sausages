 package wifi;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Random;
import java.io.PrintWriter;


import rf.RF;

public class Sender implements Runnable{
	private static final int QueueCap = 4; //Cap on how many jobs we can take

	private RF theRF;
	private int backOffWindow = theRF.aCWmin;
	private boolean ForceMaxBackOff = false;
	private Boolean[] ackQueue = new Boolean[4096];
	private ArrayBlockingQueue<Packet> sendQueue = new ArrayBlockingQueue<Packet>(QueueCap); //make this a packet object

	private short ourMAC;       // Our MAC address
	short ControlByte;
	short DestAddress;
	short SourceAddress;
	int PacketLength;
	byte[] data;
	int backoffWindow;
	int timeoutWindow = 4000; //4 seconds seems to be the magic first number
	private Packet packet;
	private PrintWriter output; //GUI thing
	int reTransmit = 0;
	Random rand = new Random();



	
	//go through the decision tree
	
	public Sender(RF theRF, Boolean[] ackQueue, ArrayBlockingQueue<Packet> sendQueue, short ourMAC, PrintWriter output) {
		this.sendQueue = sendQueue;
		this.ourMAC = ourMAC;
		this.theRF = theRF;
		this.ackQueue = ackQueue;
		this.output = output;
		
		
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
    
    private void wait(int wait){
        try{
            Thread.sleep(wait); //wait DIFS amount of time
        }
        
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }

    }
    
	private void WaitBackoffWindow(int backOff) { //waits the exponential back off time
		
		
		while(backOff>0) { //If our backoff is not done yet
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
	
    private void StartBackoff(int backoff) { //handles the waiting till transmission is open
    	boolean sent = false;
    	if(ForceMaxBackOff!=true) backoff = rand.nextInt(backOffWindow);//wait max time each
    	while(sent==false) {
    		WaitBackoffWindow(backoff);
    		DIFS();
    		if(theRF.inUse()==false) {
    			sendPacket();
    			sent = true; //we sent no longer need to be looping
    		}
    		else {
    			if(backOffWindow>=theRF.aCWmax) {
    				backOffWindow = theRF.aCWmax;
    			}
    			else backOffWindow = backOffWindow * 2; //Exponential backoff, need to fix to use frame size and not the number

    		}
    	}
    }
    
    
    private void AwaitingAck() { //if send fails the First time
    	try{
            Thread.sleep(timeoutWindow); //wait for our timeout value
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
    	if(ackQueue[packet.getSequenceNum()]==true) { //if we got an ack for our packet were done
			backOffWindow = theRF.aCWmin;//reset windows
			timeoutWindow = 3000;
    		return;
    	}
    	else {
    		if(reTransmit>=theRF.dot11RetryLimit) { //retransmit limit reached
    			reTransmit=0;
    			System.out.println("Retry Limit Reached");
    			return;
    		}
    		
    		timeoutWindow = timeoutWindow + 1000;//add 1 seconds before timeout
    		reTransmit++;
    		StartBackoff(backOffWindow); //start trying to send again
    	}
    	//get seqNumber
    	
    }
    
    
    private void sendPacket() { //sends the packet
    		theRF.transmit(packet.getPacket());
    		if(packet.desAddr!=-1) {
    			AwaitingAck();
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
						 StartBackoff(backOffWindow); //was in use so we start backing off
					 } 
				 }
				 else {
					 StartBackoff(backOffWindow); //was in use so we start backing off
				 }          
		     }	 
		}		
	}//end of run
}
