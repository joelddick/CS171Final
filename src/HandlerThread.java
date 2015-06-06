import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class HandlerThread extends Thread {
	public int QUORUM = 3;
	
	private Socket socket;
	private CommThread parentThread;
	
	public HandlerThread(CommThread t, Socket s){
		socket = s;
		parentThread = t;
	}
	
	public void run(){
		try{
			String input = null;
			Scanner socketIn = new Scanner(socket.getInputStream());
			if (socketIn.hasNext()){
				input = socketIn.nextLine();
			}
			if (input == null){
				return;
			}
			processInput(input);
			socketIn.close();
			socket.close();
		}
		catch(IOException e){
			System.out.println(e.toString());
		}
	}
	
	private void processInput(String input) throws IOException{
		
		/*
		 * ASSUME ALL MESSAGES START WITH:
		 * msgType sourceIp sourcePort
		 */
		String[] recvMsg = input.split(" ");
		String cmd = recvMsg[0];
		String sourceIp = recvMsg[1];
		int sourcePort = Integer.parseInt(recvMsg[2]);
		
		
		if(input.substring(0, 4).equals("Read")){
			String leftover = input.substring(input.indexOf(",") + 1);
			String ip = input.substring(0, leftover.indexOf(","));
			leftover = leftover.substring(input.indexOf(",") + 1);
			Integer port = Integer.valueOf(leftover.substring(0, leftover.indexOf(",")));
			read(ip, port);
		}
		else if(input.substring(0, 4).equals("Post")){
			String leftover = input.substring(input.indexOf(",") + 1);
			String ip = input.substring(0, leftover.indexOf(","));
			leftover = leftover.substring(input.indexOf(",") + 1);
			Integer port = Integer.valueOf(leftover.substring(0, leftover.indexOf(",")));
			String msg = leftover.substring(input.indexOf(",") + 1);
			
			// Get next available position in OUR log.
			int nextPosition;
			synchronized(parentThread.log) {
				nextPosition = parentThread.log.size();
			}
			
			// Create new Paxos object with nextPosition and this msg.
			parentThread.p.myVal = nextPosition;
			parentThread.p.msg = msg;
			parentThread.p.ballotNum[1] = siteId; // TODO: Figure out a way to communicate the siteId across classes.
			
			/*
			 * TODO: Deal with this section later...
			 *
			boolean won = p.start();
			if(won) {
				write(ip, port, msg);
				// TODO: Notify initiator success.
			}
			
			else {
				// TODO: Notify initiator that it failed.
			}
			 *
			 * End Todo from above.
			 */
		}
		
		// ackMsg = {ack ip port balnum balnumid acceptBalNum acceptBalNumId}
		else if(input.substring(0, 3).equals("ack")){
			String[] ackMsg = input.split(" ");
			int recvBallotNum[] = {Integer.parseInt(ackMsg[3]), Integer.parseInt(ackMsg[4])};
			int recvAcceptBallot[] = {Integer.parseInt(ackMsg[5]), Integer.parseInt(ackMsg[6])};
			int recvAcceptVal = Integer.parseInt(ackMsg[7]);
			
			if(sameBallot(recvBallotNum, parentThread.p.ballotNum)) {
				// This ack is in fact a response to my proposal. Simple check.
				if(parentThread.p.numAcks < QUORUM) {
					// If we are still trying to reach a quorum.
					if(recvAcceptVal != -1) {
						// Another site has already accepted a val.
						// Change myVal to match highest received balNum.
						if(isGreater(recvAcceptBallot, parentThread.p.ackedAcceptBal)) {
							parentThread.p.ackedAcceptBal = recvAcceptBallot;
							parentThread.p.ackedAcceptVal = recvAcceptVal;
						}
					}
					parentThread.p.numAcks++;
				}
				
				if(parentThread.p.numAcks == QUORUM) {
					// Send accept1.
					parentThread.p.sendFirstAccept();
				}
				// Else keep waiting for acks.
			} 
			
		}
		
		else if(input.substring(0, 7).equals("prepare")){
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(input.substring(8,9));
			recvBallotNum[1] = Integer.parseInt(input.substring(9,10));
			System.out.println("prepare's ballot nums: " + recvBallotNum[0] + " " + recvBallotNum[1]);
			
			if(isGreater(recvBallotNum, parentThread.p.ballotNum)) {
				// This is a higher ballot than my current, join it.
				parentThread.p.ballotNum[0] = recvBallotNum[0];
				parentThread.p.ballotNum[1] = recvBallotNum[1];
				
				// Tell leader about my latest accepted value and 
				// what value it was accepted in.
				parentThread.p.sendAck(sourceIp, sourcePort);
			}
		}
		
		// rcvMsg = accept1 ip port balNum balId val
		else if(input.substring(0, 7).equals("accept1")){
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(recvMsg[3]);
			recvBallotNum[1] = Integer.parseInt(recvMsg[4]);
			int recvVal = Integer.parseInt(recvMsg[5]);
			
			if(isGreater(recvBallotNum, parentThread.p.ballotNum)) {
				parentThread.p.acceptNum[0] = recvBallotNum[0];
				parentThread.p.acceptNum[1] = recvBallotNum[1];
				parentThread.p.acceptVal = recvVal;
				parentThread.p.sendSecondAccept(); // TODO: Only send if recvBal>ourBal ????
			}
		}
		
		else if(input.substring(0, 7).equals("accept2")){
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(recvMsg[3]);
			recvBallotNum[1] = Integer.parseInt(recvMsg[4]);
			int recvVal = Integer.parseInt(recvMsg[5]);
			
			parentThread.p.numAccept2s++;
			
			if(parentThread.p.numAccept2s == QUORUM) {
				// TODO: Decide on recvVal.
			}
		}
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
	
	private synchronized void read(String ip, Integer port) throws IOException{
		Socket s = new Socket(ip, port);
		PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
		
		for(int i = 0; i < parentThread.log.size(); i++){
			socketOut.println(parentThread.log.get(i));
		}
		
		socketOut.close();
		s.close();
	}
	
	private synchronized void write(String ip, Integer port, String msg) throws IOException{
		Socket s = new Socket(ip, port);
		PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
		
		int i = parentThread.log.size();
		parentThread.log.add(msg);
		
		socketOut.println("Success: " + i);
		
		socketOut.close();
		s.close();
	}
}
