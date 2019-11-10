package wifi;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Write a description of class Packet here.
 *
 * @author (your name)
 * @version (a version number or a date)
 */
public class Packet
{
    // TODO - control, how should this be implemented...
    private int control, CRC;
    short desAddr, scrAddr;
 
    
    
    
    byte[] packet;
    byte[] data; // should we have this?

    boolean retry;
    int frameType, sequenceNum;
    //private long data; //check, if it can be an int
    ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);

    final int MaxPacketValue = 2 + 2 + 2 + 4 + 2038;
    final int OverHeadValues = 2 + 2 + 2 + 4;

    /**
     * Constructor for objects of class Packet
     */
    public Packet() {
        packet = new byte[MaxPacketValue];// 2+2+2+2038+4 Max packet length
    }

    /**
     * Should be used by the sender as the sender packet will always have the same source address
     */
    public Packet(short scrAddr) {
        this.scrAddr = scrAddr;
    }
    
    /**
     * Contructor that I don't plan on using after creating makePacket
     */
    public Packet(short scrAddr, byte[] data, int len, short desAddr) {
        
        if(len > data.length) { // then send all of data
            if(data.length >= 2038) {   //is data bigger than max?
                //send maximum length
                packet = new byte[MaxPacketValue];
            }
            else{
                packet = new byte[OverHeadValues + data.length];
            }
        }
        else{
            //send len amount of bytes in data
            if(len > 2038) {
                //send max length
                packet = new byte[MaxPacketValue];
            }
            else{
                packet = new byte[OverHeadValues + len];
            }
        }
        setScrAddr(scrAddr);
        setDesAddr(desAddr);
        setData(data, len);
    }
    
    /**
     * Source Address sould already be set via the constructor, this is passed through when ever sender wants to create a packet
     */
    public byte[] makePacket(byte[] data, int len, short desAddr) {
        int lengthOfData = len;
        if(len > data.length) { // then send all of data
            if(data.length >= 2038) {   //is data bigger than max?
                //send maximum length
                packet = new byte[MaxPacketValue];
                lengthOfData = 2038;//maximum You'll still need to text this out...
            }
            else{
                packet = new byte[OverHeadValues + data.length];
                lengthOfData = data.length;
                System.out.println("Here");
            }
        }
        else{
            //send len amount of bytes in data
            if(len > 2038) {
                //send max length
                packet = new byte[MaxPacketValue];
            }
            else{
                packet = new byte[OverHeadValues + len];
            }
        }
        setScrAddr(scrAddr);
        setDesAddr(desAddr);
        setData(data, lengthOfData);
        setCRC1();
        return packet;
    }

    /**
     * If used by the sender class, can be used in the constructor. As sender should be send from one endsystem.
     */
    public void setScrAddr(short scrAddr) {
        this.scrAddr = (short)scrAddr;
        //put into array
        //pack[4] and pack[5]
        //a short is two bytes
        //split it up into two
        //4514 == 0b1000_1101_0001_0
        //so in the array it should show up like
        //0b0001_0001_1010_0010
        
        //pad number out to that
        int num = scrAddr | 0b0000000000000000;
        packet[4] = (byte)(num >>> 8);
        packet[5] = (byte)(0x00FF & num);
        //System.out.println("This is what packet[5] has: " + (packet[5] & 0xFF)); // I would check this to see if it works... 
        
        //(k | 0b0000000000000000) >> 8 Gets 00010001
        //(k & 0b11111111) Gets the lower half 10100010
        
        
    }

    /**
     * Sets the Destination Address in packet
     */
    public void setDesAddr(short desAddr) {
        this.desAddr = desAddr;
        int num = desAddr | 0b0000000000000000;
        packet[2] = (byte)(num >>> 8);   //shifts it 8 times
        packet[3] = (byte)(0x00FF & num); //copies the right hand side
    }
    
    /**
     * 
     */
    public void setData(byte[] data, int len) {
        //check if data can fit with in the given data space, if it's too big its invalid
        //also CRC check here

        for(int i = 0; i < len; i++) {  //This also does not check whether len exceeds data. if that's the case then send all
            //data. also I'll need to check if data can fit into the maxPacketValue.
            packet[i + 6] = data[i];
        }
        //Probably not an optimal solution. I should proably change it...
        
    }
    
    /**
     * implement later
     * just a basic one right now
     */
    public void setCRC() {
        int packOffSet = packet.length - 4;
        //
        for(int i = packOffSet; i < packet.length; i++){
            packet[i] = -1;
        }
    }
    
    /**
     * This method right now does not set the CRC, it just prints it out.
     * Currently this methos does not work correctly.
     * The numbers in the byte array does not equal crcSum.getValue
     */
    public void setCRC1() {
        //Based on CRC
        //Then call CRC once everything is done?
        CRC32 crcSum = new CRC32();
        //okay.update(packet, packet.length - 5, packet.length -1);
        crcSum.update(packet, 0, packet.length-4);
        System.out.println("CRC value calculated: " + crcSum.getValue());
        
        bb = bb.allocate(Long.BYTES);   //allocates how much buffer can hold
        //bb = bb.allocate(4); // Can't do gives overflow.
        bb.putLong(crcSum.getValue());  //makes the checksum to a byte array using byte buffer
        bb.flip();                      //set index to zero so easy to add to the array and read
        System.out.println("bb.getLong: " + bb.getLong());
        bb.flip();
        //bb.putInt(1);
        byte[] crcArray = bb.array();
        //put into the 4;
        
        bb.clear();
        System.out.println("In CRC");
        for(byte b: crcArray) { // testing print out
            System.out.println(b & 0xFF);
        }
        System.out.println();
        
        crcSum.reset();
        //change to bytes to fit into 
    }

    /**
     * 
     */
    public boolean setPacket(byte[] incomingPacket) {
        //I'll need to check this as the length of data might make this packet valid
        //
        if(incomingPacket.length >= MaxPacketValue) {// if packet is too big to fit into maxPacketLength then its invalid
            //packet = incomingPacket;
            //setControl
            //setDestination
            //setSource
            //setData
            //checkCRC
            return true;
        }
        return false;// Somthing went wrong
    }

    public int getControl() {
        //sets frameType, retry, and 
        return control;
    }

    public int getFrameType() {
        //first three bytes of the number
        
        //int holder = control;
        //byte firstHalf = (byte)(control >> 13);
        
        //if it's in a byte array
        //get first half of
        return -1;
    }

    public void setFrameType(int frameType) {
        this.frameType = 0b000; // check
    }

    public boolean getRetry() {
        return false;
    }

    public int getSequenceNum() {

        return -1;
    }

    public int getDesAddr() {

        return -1;
    }

    /**
     * 
     */
    public int getScrAddr() {
        return scrAddr;
    }

    public long getData() {

        return -1;
    }

    public void setControl(String frameType, boolean retry, 
    int sequenceNumber) {
    }

    public void setControlDefalt() {

    }
       
    /**
     * 
     */
    public byte[] returnPacket() { //based on how big the packet is... we manipulate from 0 control + addresses + data + crc;
        return packet;
    }
    
    /**
     * 
     */
    public String niceToString() {
        String str = "";
        str += "Control = " + getControl() + "\n";
        str += "\tFrame type:\t" + getFrameType() + "\n";
        str += "\tRetry:\t\t" + retry + "\n";
        str += "\tSeqNumber:\t" + getSequenceNum() + "\n";
        str += "DesAdd: " + desAddr + "\n";
        str += "SouAdd: " + scrAddr + "\n";
        //str += "Data: " + data + "\n";//change...
        str += "CRC: " + CRC + "\n";
        return str;
    }
    
    /**
     *
     */
    public String toString() {
        //System.out.println("Packet[4]: " + packet[packet.length-4] + " How much elements: " + (packet.length-4));
        String str = "[";
        for(byte B : packet) {
            str += (B & 0xFF) + ", ";
        }
        //String.format("%16s", Integer.toBinaryString(1)).replace(" ", "0");
        str += "]";
        return str;
    }     
}
