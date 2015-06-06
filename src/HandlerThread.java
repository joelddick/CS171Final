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
			post(ip, port, msg);
		}
		
		else if(input.substring(0, 7).equals("prepare")){
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(input.substring(8,9));
			recvBallotNum[1] = Integer.parseInt(input.substring(9,10));
			System.out.println("prepare's ballot nums: " + recvBallotNum[0] + " " + recvBallotNum[1]);
			
			synchronized (parentThread.p) {
				boolean returnAck = parentThread.p.checkPrepare(recvBallotNum);
				if(returnAck) {
					// TODO: Send ack back to leader, telling him about latest 
					// accepted value and what ballot was accepted in.
				}
			}
		}
		
		// ackMsg = {ack ip port balnum balnumid acceptBalNum acceptBalNumId}
		else if(input.substring(0, 3).equals("ack")) {
			String[] ackMsg = input.split(" ");
			int recvBallotNum[] = {Integer.parseInt(ackMsg[3]), Integer.parseInt(ackMsg[4])};
			int recvAcceptBallot[] = {Integer.parseInt(ackMsg[5]), Integer.parseInt(ackMsg[6])};
			int recvAcceptVal = Integer.parseInt(ackMsg[7]);
			
			synchronized (parentThread.p) {
				boolean send = parentThread.p.handleAck(recvBallotNum, recvAcceptVal, recvAcceptBallot); 
				if(send) {
					// TODO: Send accepts
				}
			}
		}
		
		// rcvMsg = accept1 ip port balNum balId val
		else if(input.substring(0, 7).equals("accept1")){
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(recvMsg[3]);
			recvBallotNum[1] = Integer.parseInt(recvMsg[4]);
			int recvVal = Integer.parseInt(recvMsg[5]);
			
			synchronized (parentThread.p) {
				boolean send2 = parentThread.p.handleAccept1(recvBallotNum, recvVal);
				if(send2) {
					// TODO: Send accept2s out
					// Only send if recvBal>ourBal ????
				}
			}
			
		}
		
		else if(input.substring(0, 7).equals("accept2")){
			int[] recvBallotNum = {0,0};
			recvBallotNum[0] = Integer.parseInt(recvMsg[3]);
			recvBallotNum[1] = Integer.parseInt(recvMsg[4]);
			int recvVal = Integer.parseInt(recvMsg[5]);
			
			boolean decide = parentThread.p.handleAccept2(recvBallotNum, recvVal);
			if(decide) {
				// TODO: Decide on this value.
			}
			
		}
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
	
	private synchronized void post(String ipAddress, Integer port, String message) throws IOException{

		boolean amLeader = false;
		synchronized(parentThread.p){
			amLeader = parentThread.p.amLeader();
		}

		if(amLeader){
			String msg = null;
			synchronized(parentThread.p) {
				msg = parentThread.p.firstAcceptMessage();
			}
			broadcast(msg);
		}
		else{
			int leader = parentThread.p.getLeader();
			Socket s = new Socket(Globals.siteIpAddresses.get(leader), Globals.sitePorts.get(leader));
			PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);

			socketOut.println("Post," + ipAddress + "," + port.toString() + "," + message);

			// TODO: If timeout then start election.
		}
	}
	
	
	private void broadcast(String msg) throws UnknownHostException, IOException {
		for(int i = 0; i < 5; i++){
			Socket s = new Socket(Globals.siteIpAddresses.get(i), Globals.sitePorts.get(i));
			PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);

			socketOut.println(msg);
		}
	}
	
	private void sendFirstAccept() throws IOException{
		for(int i = 0; i < 5; i++){
			Socket s = new Socket(Globals.siteIpAddresses.get(i), Globals.sitePorts.get(i));
			PrintWriter socketOut = new PrintWriter(s.getOutputStream(), true);

			synchronized(parentThread.p){
				socketOut.println(parentThread.p.firstAcceptMessage());
			}
		}
	}
	
	
	
}
