import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;


public class SiteProcess{
	private CommThread myComm;
	public static boolean failed = false;
	
	public SiteProcess(){
		myComm = new CommThread();
		synchronized(myComm) {
			myComm.start();
		}
		try{
			BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				if(cin.ready()){
					String input = cin.readLine();
					try {
						processInput(input);
					}
					catch (InterruptedException e) {
						System.out.println(e.toString());
					}
				}
			}
		}
		catch(IOException e) {
			System.out.println(e.toString());
		}
	}
	
	private void processInput(String input) throws InterruptedException, IOException {
		if (input.length() < 4) {
			return;
		}
		if (input.substring(0, 4).equals("Fail")) {
			if(!failed){
				System.out.println("Failing...");
				failed = true;
			}
		}
		else if (input.substring(0, 7).equals("Restore")) {
			if(failed) {
				System.out.println("Restoring...");
				failed = false;
				requestLog();
			}
		}
	}
	
	private void requestLog() throws IOException {
		askLeaderForLog(0);
		askLeaderForLog(1);
		askLeaderForLog(2);
		askLeaderForLog(3);
		askLeaderForLog(4);
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
}




