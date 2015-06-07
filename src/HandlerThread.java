import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
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
				socketIn.close();
				socket.close();
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
	
	private void processInput(String input) throws IOException {
		String[] recvMsg = input.split(",");
		String cmd = recvMsg[0];
		
		if(input.substring(0, 4).equals("Read")) {
			System.out.println("HandlerThread received Read");
			String leftover = input.substring(input.indexOf(",") + 1);
			String ip = input.substring(0, leftover.indexOf(","));
			leftover = leftover.substring(input.indexOf(",") + 1);
			Integer port = Integer.valueOf(leftover.substring(0, leftover.indexOf(",")));
			read(ip, port);
		}
		else if(input.substring(0, 4).equals("Post")) {
			System.out.println("HandlerThread received Post");
			String leftover = input.substring(input.indexOf(",") + 1);
			String ip = input.substring(0, leftover.indexOf(","));
			leftover = leftover.substring(input.indexOf(",") + 1);
			Integer port = Integer.valueOf(leftover.substring(0, leftover.indexOf(",")));
			String msg = leftover.substring(input.indexOf(",") + 1);
			post(ip, port, msg);
		}
		
		// prepare siteNum balNum balId
		else if(input.substring(0, 7).equals("prepare")) {
			System.out.println("HandlerThread received prepare");
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(recvMsg[2]);
			recvBallotNum[1] = Integer.parseInt(recvMsg[3]);
			System.out.println("prepare's ballot nums: " + recvBallotNum[0] + " " + recvBallotNum[1]);
			
			synchronized (parentThread.p) {
				boolean returnAck = parentThread.p.checkPrepare(recvBallotNum);
				if(returnAck) {
					// ack balNum, balId, acceptBalNum, acceptBalId, acceptVal
					int fromSiteId = Integer.parseInt(recvMsg[1]);
					String ackMsg = parentThread.p.getAckMsg();
					String ip = Globals.siteIpAddresses.get(fromSiteId);
					int port = Globals.sitePorts.get(fromSiteId);
					sendTo(ackMsg, ip, port);
				}
			}
		}
		
		// ackMsg = {ack balnum balnumid acceptBalNum acceptBalNumId}
		else if(input.substring(0, 3).equals("ack")) {
			System.out.println("HandlerThread received ack");
			String[] ackMsg = input.split(" ");
			int recvBallotNum[] = {Integer.parseInt(ackMsg[1]), Integer.parseInt(ackMsg[2])};
			int recvAcceptBallot[] = {Integer.parseInt(ackMsg[3]), Integer.parseInt(ackMsg[4])};
			int recvAcceptVal = Integer.parseInt(ackMsg[5]);
			
			synchronized (parentThread.p) {
				boolean send = parentThread.p.handleAck(recvBallotNum, recvAcceptBallot, recvAcceptVal); 
				if(send) {
					// accept ballotNum ballotId myval
					String acceptMsg = parentThread.p.firstAcceptMessage();
					broadcast(acceptMsg);
				}
			}
		}
		
		// rcvMsg = accept1,balNum,balId,val
		else if(input.substring(0, 7).equals("accept1")){
			System.out.println("HandlerThread received accept1");
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(recvMsg[1]);
			recvBallotNum[1] = Integer.parseInt(recvMsg[2]);
			int recvVal = Integer.parseInt(recvMsg[3]);
			
			boolean send2 = false;
			synchronized (parentThread.p) {
				send2 = parentThread.p.handleAccept1(recvBallotNum, recvVal);
			}
			if(send2) {
				String msg = null;
				synchronized(parentThread.p) {
					msg = parentThread.p.secondAcceptMessage();
				}
				if(msg != null) { broadcast(msg); }
			}
		}
		
		// rcvMsg = accept2,balNum,balId,val,msg
		else if(input.substring(0, 7).equals("accept2")){
			System.out.println("HandlerThread received accept2");
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(recvMsg[1]);
			recvBallotNum[1] = Integer.parseInt(recvMsg[2]);
			int recvVal = Integer.parseInt(recvMsg[3]);
			String msg = recvMsg[4];
			
			boolean decide = false;
			synchronized (parentThread) {
				decide = parentThread.p.handleAccept2(recvBallotNum, recvVal); 
				if(decide) {
					parentThread.log.add(recvVal, msg);
					parentThread.p.doneDeciding();
				}
			}
		}
		
		else if(recvMsg[0].equals("decide")) {
			// TODO: decide on this value, add to log, reset Paxos variables.
			// TODO: Do we want to check for majority of decides? or as soon
			// as we get one decide message.
		}
	}
	
	private synchronized void read(String ip, Integer port) throws IOException{
		System.out.println("HandlerThread read");
		Socket s = new Socket(ip, port);
		PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
		
		for(int i = 0; i < parentThread.log.size(); i++){
			socketOut.println(parentThread.log.get(i));
		}
		
		socketOut.close();
		s.close();
	}
	
	private synchronized void post(String ipAddress, Integer port, String message) throws IOException{
		System.out.println("HandlerThread post");
		boolean amLeader = false;
		boolean isDeciding = true;
		synchronized(parentThread.p){
			amLeader = parentThread.p.amLeader();
			isDeciding = parentThread.p.isDeciding();
		}

		if(amLeader){
			if(!isDeciding){
				String msg = null;
				synchronized(parentThread.p) {
					parentThread.p.prepPost(ipAddress, port, message);
					msg = parentThread.p.firstAcceptMessage();
				}
				broadcast(msg);
			}
			else{
				Socket s = new Socket(ipAddress, port);
				PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
				
				socketOut.println("Post Failed. Please try again.");
				socketOut.close();
				s.close();
			}
		}
		else{
			int leader = -1;
			synchronized(parentThread.p) {
				leader = parentThread.p.getLeader();
			}
			if(leader != -1) {
				Socket s = new Socket(Globals.siteIpAddresses.get(leader), Globals.sitePorts.get(leader));
				PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
	
				socketOut.println("Post," + ipAddress + "," + port.toString() + "," + message);
				socketOut.close();
				s.close();
			}

			// TODO: If timeout then start election.
			// prepare siteNum balNum balId
			// TODO: Add the timeout stuff...
			String prepareMsg = null;
			synchronized(parentThread.p) {
				parentThread.p.startPrepare();
				prepareMsg = parentThread.p.getPrepareMsg();
			}
			broadcast(prepareMsg);
		}
	}
	
	private void sendTo(String msg, String ip, int port) throws UnknownHostException, IOException {
		System.out.println("HandlerThread sendTo " + msg);
		Socket s = new Socket(ip, port);
		PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
		
		socketOut.println(msg);
		socketOut.close();
		s.close();
	}
	
	private void broadcast(String msg) throws UnknownHostException, IOException {
		System.out.println("HandlerThread broadcast " + msg);
		for(int i = 0; i < 5; i++){
			Socket s = new Socket(Globals.siteIpAddresses.get(i), Globals.sitePorts.get(i));
			PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);

			socketOut.println(msg);
			
			socketOut.close();
			s.close();
		}
	}
}
