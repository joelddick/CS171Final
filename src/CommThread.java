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
		}
		catch (IOException e){
			System.out.println(e.toString());
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
					System.out.println("Trying to accept.");
					if(serverSocket.isClosed()){
						serverSocket = new ServerSocket(port);
					}
					s = serverSocket.accept();
				}
				catch(IOException e){
					System.out.println(e.toString());
				}
				if(s != null){
					if(!SiteProcess.failed){
						new Thread(new HandlerThread(this, s)).start();
					}
					else{
						System.out.println("We are closed!");
						try{
							s.close();
							serverSocket.close();
							System.out.println("We are actually closed!");
						}
						catch(IOException e){
							System.out.println(e.toString());
						}
					}
				}
				s = null;
			}
		}
	}
}