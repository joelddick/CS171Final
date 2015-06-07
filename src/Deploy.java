import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Deploy {

	public static void main(String [] args) throws NumberFormatException, IOException {
		configureGlobals();
		if (args[1].equals("-c")){
			ClientProcess c = new ClientProcess();
		}
		else{
			CommThread c = new CommThread();
		}
	}
	
	private static void configureGlobals() throws NumberFormatException, IOException {
		// Read from file and get side ID
		String path = "config.txt";
		FileReader fr = new FileReader(path);
		BufferedReader br = new BufferedReader(fr);
		
		int myID = Integer.parseInt(br.readLine());		// line 1
		String site1 = br.readLine();					// line 2
		String site2 = br.readLine();					// line 3
		String site3 = br.readLine();					// line 4
		String site4 = br.readLine();					// line 5
		String site5 = br.readLine();					// line 6


		site1 = site1.substring(0, site1.indexOf(' '));
		site2 = site2.substring(0, site2.indexOf(' '));
		site3 = site3.substring(0, site3.indexOf(' '));
		site4 = site4.substring(0, site4.indexOf(' '));
		site5 = site5.substring(0, site5.indexOf(' '));
		
		br.close();
		fr.close();
		
		// Set globals
		Globals.mySiteId = myID;
		Globals.siteIpAddresses.add(site1);
		Globals.siteIpAddresses.add(site2);
		Globals.siteIpAddresses.add(site3);
		Globals.siteIpAddresses.add(site4);
		Globals.siteIpAddresses.add(site5);
	}
}
