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
		
		if(input.substring(0, 4).equals("Read")) {
			System.out.println("HandlerThread received Read");
//			String leftover = input.substring(input.indexOf(",") + 1);
//			String ip = input.substring(0, leftover.indexOf(","));
//			leftover = leftover.substring(input.indexOf(",") + 1);
//			Integer port = Integer.valueOf(leftover.substring(0, leftover.indexOf(",")));
			String ip = recvMsg[1];
			Integer port = Integer.valueOf(recvMsg[2]);
			read(ip, port);
		}
		else if(input.substring(0, 4).equals("Post")) {
			System.out.println("HandlerThread received Post");
//			String leftover = input.substring(input.indexOf(",") + 1);
//			String ip = input.substring(0, leftover.indexOf(","));
//			leftover = leftover.substring(input.indexOf(",") + 1);
//			Integer port = Integer.valueOf(leftover.substring(0, leftover.indexOf(",")));
//			String msg = leftover.substring(input.indexOf(",") + 1);
			
			String ip = recvMsg[1];
			Integer port = Integer.valueOf(recvMsg[2]);
			String msg = recvMsg[3];
			
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
			String[] ackMsg = input.split(",");
			int recvBallotNum[] = {Integer.parseInt(ackMsg[1]), Integer.parseInt(ackMsg[2])};
			int recvAcceptBallot[] = {Integer.parseInt(ackMsg[3]), Integer.parseInt(ackMsg[4])};
			int recvAcceptVal = Integer.parseInt(ackMsg[5]);
			
			synchronized (parentThread.p) {
				boolean send = parentThread.p.handleAck(recvBallotNum, recvAcceptBallot, recvAcceptVal); 
				if(send) {
					// accept1 ballotNum ballotId myval mag
					String acceptMsg = parentThread.p.firstAcceptMessage();
					System.out.println("sending " + acceptMsg);
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
		
		// recvMsg = RequestLog,sourceID
		else if(recvMsg[0].equals("RequestLog")) {
			boolean amLeader = false;
			int id = Integer.parseInt(recvMsg[1]);
			int leader = -1;
			synchronized(parentThread.p){
				amLeader = parentThread.p.amLeader();
				leader = parentThread.p.getLeader();
			}
//			if(amLeader) {
				sendLog(id);
//			}
//			else {
//				sendLeaderIsTo(leader, id);
//			}
		}
		
		else if(recvMsg[0].equals("LeaderIs")) {
			System.out.println("Asking new leader for log");
			askLeaderForLog(Integer.parseInt(recvMsg[1]));
		}
		
		else if(recvMsg[0].equals("LogIs")) {
			System.out.println("Updating my log");
			synchronized(parentThread.log) {
				for(int i = 1; i < recvMsg.length; i++) {
					while(parentThread.log.size() < recvMsg.length-1) {
						parentThread.log.add("");
					}
					parentThread.log.set(i-1, recvMsg[i]);
				}
			}
		}
		
		else if(recvMsg[0].equals("decide")) {
			// TODO: decide on this value, add to log, reset Paxos variables.
			// TODO: Do we want to check for majority of decides? or as soon
			// as we get one decide message.
		}
	}
	
	private boolean askLeaderForLog(int leader) throws IOException {
		Socket socket = new Socket();
		String leaderIp = Globals.siteIpAddresses.get(leader);
		int leaderPort = Globals.sitePorts.get(leader);
		
		try {
			socket.connect(new InetSocketAddress(leaderIp, leaderPort), 7000);
		} catch (IOException e){
			System.out.println("Client socket timeout. Trying new leader.");
			socket.close();
			return false;
		}
		
		PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);

		// RequestLog,sourceID
		socketOut.println("RequestLog," + Globals.mySiteId);

		socketOut.close();
		socket.close();
		
		return true;
	}
	
	private synchronized void sendLeaderIsTo(int leader, int originId) throws UnknownHostException, IOException {
		System.out.println("HandlerThread sendLeaderIsTo to: " + originId);
		String ip = Globals.siteIpAddresses.get(originId);
		int port = Globals.sitePorts.get(originId);
		
		Socket s = new Socket(ip, port);
		PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
		
		String leaderMsg = "LeaderIs,"+leader;
		socketOut.println(leaderMsg);
		socketOut.close();
		s.close();
	}
	
	private synchronized void sendLog(int id) throws UnknownHostException, IOException {
		System.out.println("HandlerThread sendLog to: " + id);
		String ip = Globals.siteIpAddresses.get(id);
		int port = Globals.sitePorts.get(id);
		
		Socket s = new Socket(ip, port);
		PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
		
		String logCopy = "LogIs,";
		
		synchronized(parentThread){
			for(int i = 0; i < parentThread.log.size(); i++){
				System.out.println(parentThread.log.get(i));
				logCopy += parentThread.log.get(i) + ",";
			}
		}
		socketOut.println(logCopy);
		socketOut.close();
		s.close();
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
			System.out.println("I am the Leader!");
			if(!isDeciding){
				String msg = null;
				synchronized(parentThread.p) {
					parentThread.p.prepPost(ipAddress, port, message);
					msg = parentThread.p.firstAcceptMessage();
				}
				broadcast(msg);
			}
			else{
				// I am deciding and incomingPort == currentPort, then reset, don't reject
				boolean reject = true;
				synchronized(parentThread.p) {
					if(port == parentThread.p.currentPort) {
						parentThread.p.reset();
						reject = false;
					}
				}
				if(reject) {
					// Else, I am deciding and incomingPort != currentPort, then reject
					Socket s = new Socket(ipAddress, port);
					PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
					
					socketOut.println("Post Failed. Please try again.");
					socketOut.close();
					s.close();
				}
			}
		}
		else{
			System.out.println("Not the Leader!");
			int leader = -1;
			synchronized(parentThread.p) {
				leader = parentThread.p.getLeader();
				System.out.println("Leader is " + leader);
			}
			if(leader != -1) {
				Socket s = new Socket();
				try {
					System.out.println("Trying to connect to " + leader);
					s.connect(new InetSocketAddress(Globals.siteIpAddresses.get(leader), Globals.sitePorts.get(leader)), 1000);
				} catch (IOException e){
			        System.out.println("Socket Timeout. Starting Election");
			        String prepareMsg = null;
					synchronized(parentThread.p) {
						parentThread.p.startPrepare(ipAddress, port, message);
						prepareMsg = parentThread.p.getPrepareMsg();
					}
					broadcast(prepareMsg);
					s.close();
					return;
			    }
				
				System.out.println("No Timeout");
				
				// If no timeout...
				PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);
				socketOut.println("Post," + ipAddress + "," + port.toString() + "," + message);
				socketOut.close();
				s.close();
			}

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
	
	private void broadcast(String msg) throws UnknownHostException {
		System.out.println("HandlerThread broadcast " + msg);
		for(int i = 0; i < 5; i++){
			try{
			Socket s = new Socket(Globals.siteIpAddresses.get(i), Globals.sitePorts.get(i));
			PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);

			socketOut.println(msg);
			
			socketOut.close();
			s.close();
			}
			catch (IOException e){
				// Try the next one.
			}
		}
	}
}
