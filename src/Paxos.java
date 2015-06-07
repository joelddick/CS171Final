
public class Paxos {

	public int QUORUM = 3;
	
	private int[] 	ballotNum = {0,0}; 
	private String 	msg = null;
	private int 	myVal = 0;
	private int		numAcks = 0;
	private int[]	ackedAcceptBal = {0, 0}; 	// (balNum, balNumId). Store highest received ballot
	private int		ackedAcceptVal = -1;		// and corresponding ackVal.
	private int[] 	acceptNum = {0,0};
	private int 	acceptVal = -1; // Initialize to -1 because 0 is location in log
	private int 	numAccept2s = 0;
	private int		siteId;
	private int 	leader;
	private boolean isDeciding = false;
	
	public synchronized void reset() {
		ballotNum[0] = 0;
		ballotNum[1] = 1;
		msg = null;
//		msgId = -1;
		myVal = -1;
		numAcks = 0;
		ackedAcceptBal[0] = 0;
		ackedAcceptBal[1] = 0;
		acceptNum[0] = 0;
		acceptNum[1] = 0;
		acceptVal = -1;
		numAccept2s = 0;
		isDeciding = false;
		
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
	
	// prepare siteNum balNum balId
	public synchronized String getPrepareMsg() {
		 String msg = "prepare," + 
			Globals.mySiteId + "," +
			ballotNum[0] + "," + 
			ballotNum[1];
		 return msg;
	}
	
	public synchronized void startPrepare(){
		this.ballotNum[0]++;
		this.ballotNum[1] = Globals.mySiteId;
	}
	
	/*
	 *  Check prepare returns true if recvBallotNum > this.ballotNum
	 *  letting HandlerThread know to proceed with protocol and send
	 *  and ack to source.
	 */
	public synchronized boolean checkPrepare(int[] recvBallotNum) {
		if(isGreater(recvBallotNum, this.ballotNum) || sameBallot(recvBallotNum, this.ballotNum)) {
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
	public synchronized boolean handleAck(int[] recvBallotNum, int[] recvAcceptBallot, int recvAcceptVal) {
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
						this.myVal = ackedAcceptVal;
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
		if(isGreater(recvBallotNum, this.ballotNum) || sameBallot(recvBallotNum, this.ballotNum)) {
			this.acceptNum[0] = recvBallotNum[0];
			this.acceptNum[1] = recvBallotNum[1];
			this.acceptVal = recvVal;
			return true;
		}
		return false;
	}
	
	public synchronized boolean handleAccept2(int[] recvBallotNum, int recvVal) {
		if(sameBallot(acceptNum, recvBallotNum) && acceptVal==recvVal) {
			this.numAccept2s++;
			if(this.numAccept2s == QUORUM) {
				return true;
			}
		}
		return false;
	}
	
	// ack balNum, balId, acceptBalNum, acceptBalId, acceptVal
	public synchronized String getAckMsg() {
		String msg = "ack," + 
			ballotNum[0] + "," +  	// balNum
			ballotNum[1] + "," +	// balId
			acceptNum[0] + "," +	// acceptBalNum
			acceptNum[1] + "," +	// acceptBalId
			acceptVal;	
		return msg;
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
	
	
	
	
	public synchronized boolean amLeader(){
		return siteId == leader;
	}
	
	public synchronized int getLeader(){
		return leader;
	}
	

	
	/*
	 * Cohort's perspective.
	 */
	public synchronized void sendAck(String ip, int port) {
		// TODO: Send ack to ip:port
	}
	
	/*
	 * Happens once when leader accepts new value.
	 */
	public synchronized String firstAcceptMessage() {
		return "accept1," + ballotNum[0] + "," + ballotNum[1] + "," + myVal + "," + msg;
	}
	
	/*
	 * Happens any time you get an accept1.
	 */
	public synchronized String secondAcceptMessage() {
		return "accept2," + ballotNum[0] + "," + ballotNum[1] + "," + myVal + "," + msg;
	}
	
	public synchronized void prepPost(String ipAddress, Integer port, String message){
		isDeciding = true;
		msg = message;
	}
	
	public synchronized boolean isDeciding(){
		return isDeciding;
	}
	
	public synchronized void doneDeciding(){
		isDeciding = false;
		myVal++;
	}
	
}
