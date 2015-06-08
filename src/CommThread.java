import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CommThread extends Thread{
	
	public List<String> log;
	public boolean leader;
	public Paxos p;
	
	private ServerSocket serverSocket;
	private int port = 5000;
	
	public CommThread(){
		try{
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(2000);
		}
		catch (IOException e){
			//System.out.println(e.toString());
		}
		
		p = new Paxos(); // populate in handler thread after receiving "post" if leader
		leader = false;
		log = Collections.synchronizedList(new ArrayList<String>());
	}
	
	public void run(){
		Socket s = null;
		while (true) {
			while(!SiteProcess.failed) {
				try{
					if(serverSocket.isClosed()){
						serverSocket = new ServerSocket(port);
						serverSocket.setSoTimeout(2000);
						//System.out.println("Created new serverSocket");
					}
					//System.out.println("Accepting Connection");
					s = serverSocket.accept();
				}
				catch(IOException e){
					//System.out.println(e.to
					if(SiteProcess.failed){
						try{
							serverSocket.close();
						}
						catch(IOException e2){
							//
						}
						break;
					}
				}
				if(s != null){
					if(!SiteProcess.failed){
						new Thread(new HandlerThread(this, s)).start();
					}
					else{
						try{
							s.close();
							serverSocket.close();
							System.out.println("ServerSocket Closed");
						}
						catch(IOException e){
							//System.out.println(e.toString());
						}
					}
				}
				s = null;
			}
		}
	}
}