
public class Paxos {

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
	private int 	leader;
	
	
	public Paxos(int v, String m, int si) {
		myVal = v;
		msg = m;
		siteId = si;
		ballotNum[1] = siteId;
	}
	
	public synchronized boolean amLeader(){
		return siteId == leader;
	}
	
	public synchronized int getLeader(){
		return leader;
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
	 * Cohort's perspective.
	 */
	public synchronized void sendAck(String ip, int port) {
		// TODO: Send ack to ip:port
	}
	
	/*
	 * Leader's perspective.
	 */
	public synchronized String firstAcceptMessage() {
		return "accept1," + ballotNum[0] + "," + ballotNum[1] + "," + myVal;
	}
	
	/*
	 * Everyone's perspective.
	 */
	public synchronized void sendSecondAccept() {
		// TODO: Broadcast accept2 with ballotNum and val to all
	}
	
}
