
public class Paxos {

	public int QUORUM = 3;
	
	private int[] 	ballotNum = {0,0}; 
	private String 	msg;
	private int 	myVal;
	private int		numAcks = 0;
	private int[]	ackedAcceptBal = {0, 0}; 	// (balNum, balNumId). Store highest received ballot
	private int		ackedAcceptVal = -1;		// and corresponding ackVal.
	private int[] 	acceptNum = {0,0};
	private int 	acceptVal = -1; // Initialize to -1 because 0 is location in log
	private int 	numAccept2s = 0;
	private int		siteId;
	
	
	public Paxos(int v, String m, int si) {
		myVal = v;
		msg = m;
		siteId = si;
		ballotNum[1] = siteId;
	}
	
	public synchronized boolean beginPhaseOne() {
		ballotNum[0]++;
		sendPrepare();
		return false;
	}
	
	public synchronized boolean beginPhaseTwo() {
		
		return false;
	}
	
	/*
	 * Leader's perspective.
	 */
	public synchronized void sendPrepare() {
		// TODO: Broadcast prepare messages to all
	}
	
	/*
	 *  Check prepare returns true if recvBallotNum > this.ballotNum
	 *  letting HandlerThread know to proceed with protocol and send
	 *  and ack to source.
	 */
	public synchronized boolean checkPrepare(int[] recvBallotNum) {
		if(isGreater(recvBallotNum, this.ballotNum)) {
			// This is a higher ballot than my current, join it.
			this.ballotNum[0] = recvBallotNum[0];
			this.ballotNum[1] = recvBallotNum[1];
			
			return true;
		}
		return false;
	}
	
	
	/*
	 * Returns true if we have received a majority number of acks for
	 * our ballot number letting handler thread know to broadcast 
	 * accept1. Else it returns false to tell handler thread to either
	 * keep waiting for more acks, or just to ignore it.
	 */
	public synchronized boolean handleAck(int[] recvBallotNum, int recvAcceptVal, int[] recvAcceptBallot) {
		if(sameBallot(recvBallotNum, this.ballotNum)) {
			// This ack is in fact a response to my proposal. Simple check.
			if(this.numAcks < QUORUM) {
				// If we are still trying to reach a quorum.
				if(recvAcceptVal != -1) {
					// Another site has already accepted a val.
					// Change myVal to match highest received balNum.
					if(isGreater(recvAcceptBallot, this.ackedAcceptBal)) {
						this.ackedAcceptBal = recvAcceptBallot;
						this.ackedAcceptVal = recvAcceptVal;
					}
				}
				this.numAcks++;
			}
			if(this.numAcks == QUORUM) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Returns true to let handler thread know to broadcast accept2s.
	 */
	public synchronized boolean handleAccept1(int[] recvBallotNum, int recvVal) {
		if(isGreater(recvBallotNum, this.ballotNum)) {
			this.acceptNum[0] = recvBallotNum[0];
			this.acceptNum[1] = recvBallotNum[1];
			this.acceptVal = recvVal;
			return true;
		}
		return false;
	}
	
	public synchronized boolean handleAccept2(int[] recvBallotNum, int recvVal) {
		this.numAccept2s++;
		if(this.numAccept2s == QUORUM) {
			return true;
		}
		return false;
	}
	
	// Compares two ballot numbers in the form of int arrays
	// Returns true if left > right.
	public boolean isGreater(int b1[], int b2[]) {
		return ( b1[0]>b2[0] || (b1[0]==b2[0] && b1[1]>b2[1]));
	}
		
	// Checks to see if ballot numbers are in fact the same
	// and matching
	public boolean sameBallot(int b1[], int b2[]) {
		return ( b1[0]==b2[0]  &&  b1[1]==b2[1] );
	}
	
}
