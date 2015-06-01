import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class ClientProcess {
	private int leader = 0;
	private String ipAddress = "";
	private Integer port = 5000;

	private ServerSocket serverSocket;

	public static void main(String[] args){
		ClientProcess process = new ClientProcess(args[0], Integer.valueOf(args[1]));
	}

	public ClientProcess(String ip, Integer port){
		ipAddress = ip;
		this.port = port;
		try{
			BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
			while (true){
				String input = cin.readLine();
				processInput(input);
			}
		}
		catch(IOException e){
			System.out.println(e.toString());
		}
	}

	private void processInput(String input) throws IOException{
		if(input.length() < 4){
			return;
		}
		String command = input.substring(0, 4);
		if (command.equals("Post")){
			String message = input.substring(5);
			if (message.length() > 140){
				message = message.substring(0,140);
			}
			post(message);
			List<String> response = myWait();
			System.out.println(response.get(0));
		}
		else if (command.equals("Read")){
			read();
			List<String> response = myWait();
			for (int i = 0; i < response.size(); i++){
				System.out.println(response);
			}
		}
	}

	private void post(String message) throws IOException{
		Socket socket 			= new Socket(Globals.siteIpAddresses.get(leader), Globals.sitePorts.get(leader));
		PrintWriter socketOut 	= new PrintWriter(socket.getOutputStream(), true);

		socketOut.println("Post," + ipAddress + "," + port.toString() + "," + message);

		socketOut.close();
		socket.close();
	}

	private void read() throws IOException{
		Socket socket 			= new Socket(Globals.siteIpAddresses.get(leader), Globals.sitePorts.get(leader));
		PrintWriter socketOut 	= new PrintWriter(socket.getOutputStream(), true);

		socketOut.println("Read," + ipAddress + "," + port.toString());

		socketOut.close();
		socket.close();
	}

	private ArrayList<String> myWait() throws IOException{
		serverSocket = new ServerSocket(port);
		Socket socket = serverSocket.accept();
		Scanner socketIn = new Scanner(socket.getInputStream());

		ArrayList <String> response = new ArrayList<String>();

		while (socketIn.hasNext()){
			response.add(socketIn.nextLine());
		}

		socketIn.close();
		socket.close();
		serverSocket.close();

		return response;
	}
}
