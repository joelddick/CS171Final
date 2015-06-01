import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class HandlerThread extends Thread{
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
			write(ip, port, msg);
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
