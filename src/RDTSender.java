/**
 * RDTSender : Encapsulate a reliable data sender that runs
 * over a unreliable channel that may drop and corrupt packets 
 * (but always delivers in order).
 *
 * Ooi Wei Tsang
 * CS2105, National University of Singapore
 * 12 March 2013
 */
import java.io.*;
import java.util.*;

/**
 * RDTSender receives a byte array from "above", construct a
 * data packet, and send it via UDT.  It also receives
 * ack packets from UDT.
 */
class RDTSender {
	UDTSender udt;
    int seqNumber;
    String hostname;
    int port;

	RDTSender(String hostname, int port) throws IOException
	{
		udt = new UDTSender(hostname, port);
        seqNumber = 0;
        this.hostname = hostname;
        this.port = port;
	}

	/**
	 * send() delivers the given array of bytes reliably and should
	 * not return until it is sure that the packet has been
	 * delivered.
	 */
	void send(byte[] data, int length) throws IOException, ClassNotFoundException
	{
        // send packet
		DataPacket p = new DataPacket(data, length, seqNumber);
		udt.send(p);
		
		// start timer
		Timer timeOut = new Timer();
		timeOut.schedule(new sendWithTimer(data, length, hostname, port), 100);
		
        // receive ACK
		AckPacket ack = udt.recv();
		
		// handle pkts with correct ack
		if(ack.isCorrupted==false && ack.ack==seqNumber) {
			timeOut.cancel();
			
			// alternate sequence number
			seqNumber = alterateBit(seqNumber);
		}
	}

	/**
	 * Swap the bit around
	 * i.e. 1 -> 0, 0 -> 1
	 */
	private int alterateBit(int bit) {
		assert (bit==1 || bit==0);
		if(bit==0) {
			bit = 1;
		} else {
			bit = 0;
		}
		
		return bit;
	}

	/**
	 * close() is called when there is no more data to send.
	 * This method creates an empty packet with 0 bytes and
	 * send it to the receiver, to indicate that there is no
	 * more data.
	 * 
	 * This method should not return until it is sure that
	 * the empty packet has been delivered correctly.  It 
	 * catches any EOFException (which signals the receiver
	 * has closed the connection) and close its own connection.
	 */
	@SuppressWarnings("unused")
	void close() throws IOException, ClassNotFoundException
	{
		DataPacket p = new DataPacket(null, 0, seqNumber);
		udt.send(p);
		try {
			AckPacket ack = udt.recv();
		} catch (EOFException e) {
        } 
		finally {
			udt.close();
		}
	}
}

class sendWithTimer extends TimerTask {
	byte[] data;
	int length;
	RDTSender rdt;
	
	public sendWithTimer(byte[] data, int length, String hostname, int port) {
		this.data = data;
		this.length = length;
		try {
			rdt = new RDTSender(hostname, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	/**
	 * run() is called when timer timeouts.
	 * Packet is retransmitted and timer is restarted.
	 */
	public void run() {
		try {
			rdt.send(data, length);
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
}
