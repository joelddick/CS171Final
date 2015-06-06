import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class CommThread extends Thread{
	
	public static List<String> log;
	public static boolean leader;
//	public static List<Paxos> paxies;
	public static Paxos p;
	
	private ServerSocket serverSocket;
	private int port = 5000;
	
	public CommThread(){
		try{
			serverSocket = new ServerSocket(port);
		}
		catch (IOException e){
			System.out.println(e.toString());
		}
		
		p = new Paxos(0, null, 0); // populate in handler thread after receiving "post" if leader
		leader = false;
		log = Collections.synchronizedList(new ArrayList<String>());
	}
	
	public void run(){
		Socket s = null;
		while (true){
			try{
				 s = serverSocket.accept();
			}
			catch(IOException e){
				System.out.println(e.toString());
			}
			if(s != null){
				new Thread(new HandlerThread(this, s)).start();
			}
			s = null;
		}
	}
}