/**
 * RDTReceiver : Encapsulate a reliable data receiver that runs
 * over a unreliable channel that may drop packets (but is
 * corruption-free and delivers in order).
 *
 * Ooi Wei Tsang
 * CS2105, National University of Singapore
 * 1 March 2011
 */

import java.io.*;

/**
 * RDTReceiver receives a data packet from "below" and pass
 * the byte array contained it it to the "above".
 */
class RDTReceiver {
	UDTReceiver udt;
	int seqNumber;

	RDTReceiver(int port) throws IOException
	{
		udt = new UDTReceiver(port);
		seqNumber = 0;
	}

	/**
	 * recv() reads the next in-order, uncorrupted data packet 
	 * from the layer below and returns the byte array contains 
	 * the data.  It returns null if when there is no more data 
	 * to read (i.e., the transmission is complete).
	 */
	byte[] recv() throws IOException, ClassNotFoundException
	{
		DataPacket p = udt.recv();
		
		// validate received pkt and send respective ack
		while(p.isCorrupted==true || p.seq!=seqNumber) {		// either pkt is corrupted or wrong pkt has wrong seq
			// send previous ack
			udt.send(new AckPacket(alternateBit(seqNumber)));
			
			// wait for pkt again
			p = udt.recv();
		}

		// send correct ack
		assert p.seq == seqNumber;
		udt.send(new AckPacket(p.seq));

        // deliver data
		if (p.length > 0) {
			byte [] copy = new byte[p.length];
			System.arraycopy(p.data, 0, copy, 0, p.length);
			return copy;
		} else {
			return null;
		}
	}
	
	/**
	 * Swap the bit around
	 * i.e. 1 -> 0, 0 -> 1
	 */
	private int alternateBit(int bit) {
		assert (bit==1 || bit==0);
		if(bit==0) {
			bit = 1;
		} else {
			bit = 0;
		}
		
		return bit;
	}

	void close() throws IOException
	{
		udt.close();
	}
}
