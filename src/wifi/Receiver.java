package wifi;

 
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

public class Receiver implements Runnable{
	
	public static Queue<byte[]> DataQueue = new ArrayBlockingQueue<byte[]>(0);
	//this will end up being a packet object
	
	private RF theRF;
	private Queue<Boolean> ackQueue = new ArrayBlockingQueue<Boolean>(0);

	
	
	
	private void SIFS() {
        try{
            Thread.sleep(RF.aSlotTime); //wait SIFS amount of time
        }
        
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
	}
	
	public static byte[] popQueue() {
			return DataQueue.poll(); //return the oldest data

	}
	
	
	public Receiver(RF theRF, Queue<Boolean> ackQueue) {
		this.theRF = theRF;
		this.ackQueue = ackQueue;
	}
	
	//receiver loop
	
	//send section for ACKS//AWKS

	public  void run() {
		System.out.println("Receiver Thread Reporting For Duty!!");

		while(true) { //loop to keep thread alive
			byte[] IncomingPacket = theRF.receive();
			DataQueue.add(IncomingPacket); //put packet into queue
		}
		
	}
	
}
