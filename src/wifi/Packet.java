package wifi;

 
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * This class represents CS325's modified 802.11 Frame Format
 * -----------------------------------------------------------------
 * |Control 2B | DestAddr 2B| SrcAddr 2B | Data 0 - 2038B | CRC 4B |
 * ----/\-----------------------------------------------------------
 * ----  -------------------------------------------
 * |Frame Type 3b | Retry 1b | Sequence Number 12b |
 * -------------------------------------------------
 * 
 * Help
 * @students Anna, Hunter, Ana-Lea
 * @version uuuuhhhh
 */
public class Packet
{
    // TODO 

	private int control;
	private short retry;
	private short frameType;
	private short sequenceNum;
	public short desAddr;
	public short scrAddr;
	public byte[] data;
	private long CRCnum;
	
	public byte[] packet;
	private CRC32 CRC;

    final int MAX_PACKET_SUM = 2 + 2 + 2 + 4 + 2038;
    final int OVERHEAD_SUM = 2 + 2 + 2 + 4;
    
    final int CONTROL_START = 0;
    final int DES_START = 2;
    final int SCR_START = 4;
    final int DATA_START = 6;
    //final int CRC_START = packet.length - 4;
    
    final short DATA =      0b000;
    final short ACK =       0b001;
    final short Beacon =    0b010;
    final short CTS =       0b100;
    final short RTS =       0b101;
    
    
    ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
    
    /**
     * Creates a Packet Object
     */
    public Packet() {
    }
    
    /**
     * Takes in a byte array and turns that into a packet.
     * 
     * 
     * @param incomingPacket	a byte array
     */
    public Packet(byte[] incomingPacket) {
    	//check CRC // Ignore if CRC is not correct
    	
    	packet = new byte[incomingPacket.length];
    	
    	setControl(incomingPacket[CONTROL_START], incomingPacket[CONTROL_START + 1]);
        setScrAddr(twoBytesToShort(incomingPacket[SCR_START], incomingPacket[SCR_START+1]));
        setDesAddr(twoBytesToShort(incomingPacket[DES_START], incomingPacket[DES_START + 1]));
        setData(incomingPacket, (incomingPacket.length - OVERHEAD_SUM));

    } 
    
    /**
     * Constructor or class Packet
     */
    public Packet(short frameType, short retry, short sequenceNum, short desAddr, short scrAddr, byte[] data, int len) {
    	
        if(len > data.length) { // then send all of data
            if(data.length >= 2038) {   //is data bigger than max?
                //send maximum length
                packet = new byte[MAX_PACKET_SUM];
            }
            else{
                packet = new byte[OVERHEAD_SUM + data.length];
            }
        }
        else{
            //send len amount of bytes in data
            if(len > 2038) {
                //send max length
                packet = new byte[MAX_PACKET_SUM];
            }
            else{
                packet = new byte[OVERHEAD_SUM + len];
            }
        }
        setControl(frameType, retry, sequenceNum);
        setScrAddr(scrAddr);
        setDesAddr(desAddr);
        setData(data, len);
        setCRC();
    }
    
    /**
     * Used by receiver to translate bytes to information about control.
     */
    private void setControl(byte fByte, byte sByte) {
    	packet[0] = fByte;
    	packet[1] = sByte;
    	
        frameType = (short)((fByte >> 5)& 0x7);
        retry = (short)((fByte >> 4) & 0x1);
        byte firstHalfOfSequence = (byte) (fByte & 0xF);
        sequenceNum = twoBytesToShort(firstHalfOfSequence, sByte);
        
        
        //This doesn't set the this objects control packet byte array. Implement if needed...
    }
    
    //Setters
    
    /**
     * Used by the sender, to put data into packet
     */
    public void setControl(short frameType, short retry, short sequenceNum) {
        this.frameType = frameType;
        this.retry = retry;
        this.sequenceNum = sequenceNum;
        
        //To implement - limit on sequence number?
        
        byte holderByte = (byte)(frameType);        
        
        if(retry > 0) {
            holderByte = (byte)(((holderByte << 5 | 0b10000)) | (sequenceNum >> 8));
            System.out.println("Here " + Integer.toBinaryString(holderByte & 0xFF));
            packet[0] = holderByte;
            
        }
        else{
            holderByte = (byte)(((holderByte << 5 | 0b00000)) | (sequenceNum >> 8));
            System.out.println("Here " + Integer.toBinaryString(holderByte & 0xFF));
            packet[0] = holderByte;            
        }
        
        
        holderByte = (byte)((byte)sequenceNum & 0xFF);
        System.out.println("Here " + Integer.toBinaryString(holderByte & 0xFF));
        //byte sequenceNumByte = (byte)(sequenceNum);

        packet[1] = holderByte;

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
    
    private void setScrAddr(short scrAddr, byte[] packet) {
        
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
            packet[i + DATA_START] = data[i];
        }
        //Probably not an optimal solution. Should I change it...
        
    }
    
    
    /**
     * implement later
     * just a basic one right now
     */
    public void setCRC() {
        int packOffSet = packet.length - 4;
        System.out.println("HERE");
        for(int i = packOffSet; i < packet.length; i++){
            packet[i] = (byte)0xFF;
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
        byte[] tempArray;
        if(incomingPacket.length <= MAX_PACKET_SUM) {// if packet is too big to fit into maxPacketLength then its invalid
            packet = incomingPacket;
            //setControl(incomingPacket);
            //setControl
            //setDestination
            //setSource
            //setData
            //checkCRC
            
            return true;
        }
        
        return false;// packet is too big to fit into maxPacketLength its invalid
    }
    
    //Getters
    public int getControl() {
        //sets frameType, retry, and 
        return control;
    }
    
    
    /**
     * 
     */
    public short getFrameType() {
        return frameType;
    }

    public void setFrameType(int frameType) {
        this.frameType = 0b000; // check
    }

    /**
     * 
     */
    public short getRetry() {
        return retry;
    }

    /**
     * 
     */
    public int getSequenceNum() {

        return sequenceNum;
    }

    public int getDesAddr() {

        return desAddr;
    }

    /**
     * 
     */
    public int getScrAddr() {
        return scrAddr;
    }

    public byte[] getData() {

        return data;
    }

    public void setControl(String frameType, boolean retry, 
    int sequenceNumber) {
    }

    public void setControlDefalt() {

    }
       
    /**
     * Return
     */
    public byte[] getPacket() { //based on how big the packet is... we manipulate from 0 control + addresses + data + crc;
        return packet;
    }
    
    /**
     * Let's make life easier by doing this.
     * 
     * @param b1
     * @param b2
     * @return 
     */
    public static short twoBytesToShort(byte b1, byte b2) {
        return (short) ((b1 << 8) | (b2 & 0xFF));
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