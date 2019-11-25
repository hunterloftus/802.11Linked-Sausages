package wifi;

 
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
 * @students Anna, Hunter, Ana-Lea
 */
public class Packet
{
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
    
    //Deletevvv
    final short DATA =      0b000;
    final short ACK =       0b001;
    final short Beacon =    0b010;
    final short CTS =       0b100;
    final short RTS =       0b101;
    
    
    ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
    
    /**
     * Creates a Packet Object with nothing instantiated
     */
    public Packet() {
    	
    }
    
    /**
	 * Takes in a byte array and turns that into a packet. This constructor sets the
	 * following variables to handle all the bit manipulations
	 * 		- Control, Along with 
	 * 			- Frame Type 
	 * 			- Retry 
	 * 			- Sequence Number 
	 * 		- Source Address
	 * 		- Destination Address
	 * 		- Data within Packet.
	 * This does not set the variable CRC
	 * 
	 * 
	 * @param incomingPacket a byte array
	 */
    public Packet(byte[] incomingPacket) {    	
    	//packet = new byte[incomingPacket.length];
    	packet = incomingPacket;
    	setControl(incomingPacket[CONTROL_START], incomingPacket[CONTROL_START + 1]);
        setScrAddr(twoBytesToShort(incomingPacket[SCR_START], incomingPacket[SCR_START+1]));
        setDesAddr(twoBytesToShort(incomingPacket[DES_START], incomingPacket[DES_START + 1]));
        setData(incomingPacket);
    } 
    
    /**
	* This Constructor takes in the following parameters and sets those parameters
	* to the associated variables.
	* 
	* @param frameType 		apart of the control, this should be set to either 
	* 		- 0b000 for DATA
	* 		- 0b001 for ACK
	* 		- 0b010 for BEACON 
	* 		- 0b100 for CTS
	* 		- 0b101 for RTS
	* @param retry 			set to 1 if this is a packet that is resent, 0 if not.
	* @param sequenceNum	Sequence number of this particular packet
	* @param desAddr		Destination on this packet
	* @param scrAddr		Source address, where the packet came from. This end system's MAC address.
	* @param data			Data to send in this packet. 
	* @param len			Length of data in data to send.
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
        setCRC();	//Sets to all ones.
        //setCRC1(); Doesn't work correctly
    }
    //Setters for Packet Class -----------------------------------------------------------------

    /**
	* Used by receiver to translate bytes to information about control by setting the var for control.
	* Takes the first two bytes of a frame, concatenates those two bytes together to get the control and it's sub-parts frameType, retry, sequenceNumber. 
	* 
	* @param fByte first B in the array
	* @param sByte	Second B in the array
	*/
    private void setControl(byte fByte, byte sByte) {
    	packet[0] = fByte;
    	packet[1] = sByte;
    	
        frameType = (short)((fByte >> 5)& 0x7);
        retry = (short)((fByte >> 4) & 0x1);
        byte firstHalfOfSequence = (byte) (fByte & 0xF);
        sequenceNum = twoBytesToShort(firstHalfOfSequence, sByte);
        control = twoBytesToShort(fByte, sByte);
    }
    
    /**
	* Used by the sender constructor, to put data into packet
	* 
	* @param frameType apart of the control, this should be set to either 
	* 			- 0b000 for DATA
	* 			- 0b001 for ACK
	* 			- 0b010 for BEACON 
	* 			- 0b100 for CTS
	* 			- 0b101 for RTS
	* @param retry set to 1 if this is a packet that is resent
	* @param sequenceNum	Sequence number of this particular packet
	*/
    public void setControl(short frameType, short retry, short sequenceNum) {
        this.frameType = frameType;
        this.retry = retry;
        this.sequenceNum = sequenceNum;
        
        //To implement - limit on sequence number?
        
        byte holderByte = (byte)(frameType);        
        
        if(retry > 0) {
            holderByte = (byte)(((holderByte << 5 | 0b10000)) | (sequenceNum >> 8));
            packet[0] = holderByte;
            
        }
        else{
            holderByte = (byte)(((holderByte << 5 | 0b00000)) | (sequenceNum >> 8));
            packet[0] = holderByte;            
        }
        
        holderByte = (byte)((byte)sequenceNum & 0xFF);	// Gets the last of the sequence number
        packet[1] = holderByte;

    }
    

    
    /**
	* Passes along a Source address short to be turned into Bytes then placed
	* in a frame - indexed at packer[4], packet[5].
	* 
	* @param scrAddr short, the is the source address that a packet originates from.
	*/
    public void setScrAddr(short scrAddr) {
        this.scrAddr = (short)scrAddr;
        int num = scrAddr | 0b0000000000000000;
        packet[4] = (byte)(num >>> 8);
        packet[5] = (byte)(0x00FF & num);
    }
    

    /**
    * Sets the Destination Address in packet
    * 
    * @param desAddres destination address that the packet was intended to be sent to.
    */
    public void setDesAddr(short desAddr) {
        this.desAddr = desAddr;
        int num = desAddr | 0b0000000000000000;
        packet[2] = (byte)(num >>> 8);   //shifts it 8 times
        packet[3] = (byte)(0x00FF & num); //copies the right hand side
    }
    

    /**
    * Passes through a byte array to fill the data to a specific length specified through the param len.
    * Used by the sender.
    * 
    * @param data	Data of a packet
    * @param len	length of byte to send of the byte array passed through the param data
    */
    public void setData(byte[] data, int len) {
        for(int i = 0; i < len; i++) {
            packet[i + DATA_START] = data[i];
        }        
    }
    
    /**
     * 
     */
    public void setData(byte[] incomingPacket) {
    	byte[] data = new byte[incomingPacket.length - OVERHEAD_SUM];
    	//byte[] data = new byte[incomingPacket.length];
    	//System.out.println(tempArray.length);
    	//packet is already set, now I need to set the data.
    	for(int i = 0; i < data.length; i++) {
    		//System.out.println("i at: " + i);
    		data[i] = incomingPacket[i + DATA_START];
    		//System.out.println("I want to die " + incomingPacket[i + DATA_START]);
    		
            //packet[i + DATA_START] = data[i];
        } 
    	this.data = data;
    }
    
    
    /**
    * Alternative CRC, that fills the CRC with all 1's (-1's)
    */
    public void setCRC() {
        int packOffSet = packet.length - 4;
        for(int i = packOffSet; i < packet.length; i++){
            packet[i] = (byte)0xFF;
        }
    }
    
    /**
    * This method right now does not set the CRC, it just prints it out.
    * Currently this method does not work correctly.
    * The numbers in the byte array does not equal crcSum.getValue
    */
    public void setCRC1() {
        CRC32 crcSum = new CRC32();
        crcSum.update(packet, 0, packet.length-5);
        CRCnum = crcSum.getValue();
        System.out.println("CRC value calculated: " + crcSum.getValue());
        bb.putLong(crcSum.getValue());
        
        byte[] bbb = bb.array();
        for(Byte b: bbb) {
        	System.out.println(b);
        	
        }
        bb.flip();
        //byte[] bbb = bb.array();
        int start = bbb.length - 5;
        for(int i = 0; i < bbb.length; i ++) {
        	System.out.println("i: " + i );
        	System.out.println(bbb[i] & 0xFF);
        	
        }
        //We want the last four of bbb;
        
        int packOffSet = packet.length - 5;
        for(int i = packOffSet; i < packet.length; i++){
            packet[i] = (byte)(bbb[start]);
            start++;
        }
        
        crcSum.reset();
        //change to bytes to fit into 
    }
    
    /**
     * Use this method if the current packet is a retransmission.
     * This method sets the retry bit in Control to 1 (true);
     * 
     */
    public void retransmition() {
    	setControl(frameType, (short)1, sequenceNum);
    }
    
    /**
     * 
     */
    public void setACK() {
    	setControl((short)0b001, (short) 0, sequenceNum);
    }


    
    //Getters -----------------------------------------------------------------------------------------------------
    /**
    * 
    * @return the control of a current pacet
    */
    public int getControl() {
        return control;
    }
    
    
    /**
    * @return the frameType of a given pack, this is a short
    */
    public short getFrameType() {
        return frameType;
    }



    /**
    * @return Whether this packet is a retransmission
    * 1 if it is a retransmission
    * 0 is this is not a retransmission
    */
    public short getRetry() {
        return retry;
    }

    /**
    * @return the sequence number of this packet
    */
    public int getSequenceNum() {
        return sequenceNum;
    }

    /**
    * 
    * @return the destination address of this particular fram. packet[2] and packet[3]
    */
    public short getDesAddr() {

        return desAddr;
    }

    /**
    * 
    * @return the source Address with in the packet. packet[4] and packet[5]
    */
    public short getScrAddr() {
        return scrAddr;
    }

    /**
    * 
    * @return the data with in this packet.
    */
    public byte[] getData() {
        return data;
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
     * @return a nice String that prints out
     * 	- Control
     * 		- Frame Type
     * 		- Retry
     * 		- Sequence Number
     * 	- Destination Address
     * 	- Source Address
     */
    public String niceToString() {
    	String s = new String(data);
        String str = "";
        str += "Control = " + control + "\n";
        str += "\tFrame type:\t" + frameType + "\n";
        str += "\tRetry:\t\t" + retry + "\n";
        str += "\tSeqNumber:\t" + sequenceNum + "\n";
        str += "DesAdd: " + desAddr + "\n";
        str += "SouAdd: " + scrAddr + "\n";
        str += "Data: " + s + "\n";//change...
        str += "CRC: " + CRC + "\n";
        return str;
    }
    
    /**
     *@return Prints out the bytes that is currently in the byte array (variable packet).
     */
    public String toString() {
        String str = "[";
        for(byte B : packet) {
            str += (B & 0xFF) + ", ";
        }
        str += "]";
        return str;
    }     
}