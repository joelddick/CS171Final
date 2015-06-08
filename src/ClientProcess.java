import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientProcess {
	private int leader = 0;
	private String ipAddress = "";
	private Integer port = 5001;
	private List<String> response;

	private ServerSocket serverSocket;

	public ClientProcess() {
		ipAddress = Globals.siteIpAddresses.get(Globals.mySiteId);
		this.port += Globals.mySiteId;
		try {
			BufferedReader cin = new BufferedReader(new InputStreamReader(
					System.in));
			while (true) {
				String input = cin.readLine();
				processInput(input);
			}
		} catch (IOException e) {
			System.out.println(e.toString());
		}
	}

	private void processInput(String input) throws IOException {
		if (input.length() < 4) {
			return;
		}
		String command = input.substring(0, 4);
		if (command.equals("Post")) {
			String message = input.substring(5);
			if (message.length() > 140) {
				message = message.substring(0, 140);
			}
			while(!post(message)){
				leader = (leader+1)%5;
			}
			while(!myWait()){
				leader = (leader+1)%5;
				post(message);
			}
			if(response != null) {
				System.out.println(response.get(0));
			}
		} else if (command.equals("Read")) {
			read();
			System.out.println("Waiting for Read");
			
			while(!read()){
				leader = (leader+1)%5;
			}
			while(!myWait()){
				leader = (leader+1)%5;
				read();
			}
			
			if(response != null) {
				for (int i = 0; i < response.size(); i++) {
					System.out.println(response.get(i));
				}
			}
		}
	}

	private boolean post(String message) throws IOException {
		
		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(Globals.siteIpAddresses.get(leader), Globals.sitePorts.get(leader)), 5000);
		} catch (SocketTimeoutException e){
			System.out.println("Client socket timeout. Trying new leader.");
			socket.close();
			return false;
		}
		
		PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
		socketOut.println("Post," + ipAddress + "," + port.toString() + "," + message);

		socketOut.close();
		socket.close();
		return true;
	}

	private boolean read() throws IOException {
		
		Socket socket = new Socket(Globals.siteIpAddresses.get(leader), Globals.sitePorts.get(leader));
		PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);

		socketOut.println("Read," + ipAddress + "," + port.toString());

		socketOut.close();
		socket.close();
		
		return true;
	}

	private boolean myWait() throws IOException {
		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(5000);
		System.out.println("Waiting for accept.");
		try {
			Socket socket = serverSocket.accept();
			Scanner socketIn = new Scanner(socket.getInputStream());
	
			response = new ArrayList<String>();
			
			System.out.println("Accepted but waiting for something");
			
			while (!socketIn.hasNext()) {
				// Wait
			}
	
			
			String blog = socketIn.nextLine();
			System.out.println("blog is: " + blog);
			if(blog != null) {
				String[] posts = blog.split(",");
				for(String post : posts) {
					response.add(post);
				}
			}
			else {
				System.out.println("Blog empty!");
			}
	
			socketIn.close();
			socket.close();
			serverSocket.close();
			return true;
			
		} catch (SocketTimeoutException e){
			System.out.println("Something went wrong, please try again.");
			// Select new leader.
			
		}
		serverSocket.close();
		return false;
	}
}
