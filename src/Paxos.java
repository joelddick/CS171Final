
public class Paxos {

	public int[] 	ballotNum = {0,0}; 
	public String 	msg;
	public int 		myVal;
	public int		numAcks = 0;
	public int[]	ackedAcceptBal = {0, 0}; 	// (balNum, balNumId). Store highest received ballot
	public int		ackedAcceptVal = -1;		// and corresponding ackVal.
	
	public int[] 	acceptNum = {0,0};
	public int 		acceptVal = -1; // Initialize to -1 because 0 is location in log
	public int 		numAccept2s = 0;
	
	private int		siteId;
	
	
	public Paxos(int v, String m, int si) {
		myVal = v;
		msg = m;
		siteId = si;
		ballotNum[1] = siteId;
	}
	
	public boolean beginPhaseOne() {
		ballotNum[0]++;
		sendPrepare();
		return false;
	}
	
	public boolean beginPhaseTwo() {
		
		return false;
	}
	
	/*
	 * Leader's perspective.
	 */
	public void sendPrepare() {
		// TODO: Broadcast prepare messages to all
	}
	
	/*
	 * Cohort's perspective.
	 */
	public void sendAck(String ip, int port) {
		// TODO: Send ack to ip:port
	}
	
	/*
	 * Leader's perspective.
	 */
	public void sendFirstAccept() {
		// TODO: After receiving majority acks, broadcast accept1
		// message with ballotNum and myVal
	}
	
	/*
	 * Everyone's perspective.
	 */
	public void sendSecondAccept() {
		// TODO: Broadcast accept2 with ballotNum and val to all
	}
	
}
