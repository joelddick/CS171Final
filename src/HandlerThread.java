import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
		
		if(SiteProcess.failed && (recvMsg[0].equals("Post") || recvMsg[0].equals("Read"))) {
			String ip = recvMsg[1];
			Integer port = Integer.valueOf(recvMsg[2]);
			System.out.println("Calling sendImDead 1");
			sendImDead(ip, port);
		}
		
		else if(input.substring(0, 4).equals("Read")) {
			System.out.println("HandlerThread received Read");
			String ip = recvMsg[1];
			Integer port = Integer.valueOf(recvMsg[2]);
			read(ip, port);
		}
		
		else if(input.substring(0, 4).equals("Post")) {
			System.out.println("HandlerThread received Post");
			String ip = recvMsg[1];
			Integer port = Integer.valueOf(recvMsg[2]);
			String msg = recvMsg[3];
			
			if(!SiteProcess.failed) {
				System.out.println("Post: " + " " + ip + " " + port + " " + msg);
				post(ip, port, msg);
			} else {
				System.out.println("Calling sendImDead 2");
				sendImDead(ip, port);
			}
		}
		
		else if(recvMsg[0].equals("PostForward")) {
			//"PostForward, mySiteId, ipAddress, port, message
			System.out.println("HandlerThread received PostForward");
			int forwardingSite = Integer.parseInt(recvMsg[1]);
			String originIp = recvMsg[2];
			int originPort = Integer.parseInt(recvMsg[3]);
			String msg = recvMsg[4];
			
			if(!SiteProcess.failed) {
				System.out.println("PostForward: " + " " + originIp + " " + originPort + " " + msg);
				post(originIp, originPort, msg);
			} else {
				System.out.println("Calling sendImDead 3");
				sendImDead(Globals.siteIpAddresses.get(forwardingSite), Globals.sitePorts.get(forwardingSite));	
			}
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
		
		// rcvMsg = accept1,balNum,balId,val,msg
		else if(input.substring(0, 7).equals("accept1")){
			System.out.println("HandlerThread received accept1");
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(recvMsg[1]);
			recvBallotNum[1] = Integer.parseInt(recvMsg[2]);
			int recvVal = Integer.parseInt(recvMsg[3]);
			String message = recvMsg[4];
			
			boolean send2 = false;
			synchronized (parentThread.p) {
				send2 = parentThread.p.handleAccept1(recvBallotNum, recvVal, message);
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
				decide = parentThread.p.handleAccept2(recvBallotNum, recvVal, msg); 
				if(decide) {
					if(parentThread.p.amLeader()){
						Socket s = new Socket(parentThread.p.currentIp, parentThread.p.currentPort);
						PrintWriter socketOut = new PrintWriter(s.getOutputStream());
						
						socketOut.println("Post Successful!");
						
						socketOut.close();
						s.close();
					}
					System.out.println("Adding: " + msg + " " + recvVal);
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
		
		else if(recvMsg[0].equals("DEAD")) {
			// Start election.
		    System.out.println("Site " + recvMsg[1] + " is dead. Starting Election");
		    String prepareMsg = null;
			synchronized(parentThread.p) {
				parentThread.p.startPrepare();
				prepareMsg = parentThread.p.getPrepareMsg();
			}
			broadcast(prepareMsg);
		}
		
	}
	
	private synchronized void read(String ip, Integer port) throws IOException{
		System.out.println("HandlerThread read");
		Socket s = new Socket(ip, port);
		PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
		
		String readMsg = "";
		
		synchronized(parentThread){
			for(int i = 0; i < parentThread.log.size(); i++){
				System.out.println(parentThread.log.get(i));
				//socketOut.println(parentThread.log.get(i));
				readMsg += i + " " + parentThread.log.get(i) + ",";
			}
		}
		System.out.println("Sending Read msg as: " + readMsg);
		socketOut.println(readMsg);
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
				System.out.println("I'm leader! Sending accept");
				String msg = null;
				synchronized(parentThread.p) {
					parentThread.p.prepPost(ipAddress, port, message);
					msg = parentThread.p.firstAcceptMessage();
				}
				broadcast(msg);
			}
			else{
				System.out.println("I'm leader! But am already deciding. Reject.");
				Socket s = new Socket(ipAddress, port);
				PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
				
				socketOut.println("Post Failed. Please try again.");
				socketOut.close();
				s.close();
			}
		}
		else{
			// Am not leader, need to forward Post 
			int leader = -1;
			synchronized(parentThread.p) {
				leader = parentThread.p.getLeader();
			}
			System.out.println("I'm NOT leader. Trying to forward to " + leader);
			if(leader != -1) {
				Socket s = new Socket();
				try {
					s.connect(new InetSocketAddress(Globals.siteIpAddresses.get(leader), Globals.sitePorts.get(leader)), 5000);
				} catch (SocketTimeoutException e){
			        System.out.println("Socket Timeout. Starting Election");
			        String prepareMsg = null;
					synchronized(parentThread.p) {
						parentThread.p.startPrepare();
						prepareMsg = parentThread.p.getPrepareMsg();
					}
					broadcast(prepareMsg);
					s.close();
					return;
			    }
				
				// If no timeout...
				System.out.println("I'm NOT leader. Forwarding to " + leader);
				PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
				socketOut.println("PostForward," + "," + Globals.mySiteId + "," + ipAddress + "," + port.toString() + "," + message);
				socketOut.close();
				s.close();
				System.out.println("Forwarded " + "PostForward," + "," + Globals.mySiteId + "," + ipAddress + "," + port.toString() + "," + message);
			}

		}
	}
	
	private void sendImDead(String ip, Integer port) throws UnknownHostException, IOException {
		System.out.println("Sending DEAD");
		String deadMsg = "DEAD," + Globals.mySiteId;
		sendTo(deadMsg, ip, port);
	}
	
	private void sendTo(String msg, String ip, int port) throws UnknownHostException, IOException {
		System.out.println("HandlerThread sendTo " + msg + " " + ip);
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
