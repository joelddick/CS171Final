import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Deploy {

	public static void main(String [] args) throws NumberFormatException, IOException {
		configureGlobals();
		if (args[0].equals("-c")){
			ClientProcess c = new ClientProcess();
		}
		else if(args[0].equals("-s")){
			SiteProcess s = new SiteProcess();
		}
		else{
			System.out.println("Please Try Again");
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


		String site1s = site1.substring(0, site1.indexOf(' '));
		String site2s = site2.substring(0, site2.indexOf(' '));
		String site3s = site3.substring(0, site3.indexOf(' '));
		String site4s = site4.substring(0, site4.indexOf(' '));
		String site5s = site5.substring(0, site5.indexOf(' '));
		
		Integer port1 = Integer.valueOf(site1.substring(site1.indexOf(' ')+1));
		Integer port2 = Integer.valueOf(site2.substring(site2.indexOf(' ')+1));
		Integer port3 = Integer.valueOf(site3.substring(site3.indexOf(' ')+1));
		Integer port4 = Integer.valueOf(site4.substring(site4.indexOf(' ')+1));
		Integer port5 = Integer.valueOf(site5.substring(site5.indexOf(' ')+1));
		
		br.close();
		fr.close();
		
		// Set globals
		Globals.mySiteId = myID;
		Globals.siteIpAddresses.add(site1s);
		Globals.siteIpAddresses.add(site2s);
		Globals.siteIpAddresses.add(site3s);
		Globals.siteIpAddresses.add(site4s);
		Globals.siteIpAddresses.add(site5s);
		Globals.sitePorts.add(port1);
		Globals.sitePorts.add(port2);
		Globals.sitePorts.add(port3);
		Globals.sitePorts.add(port4);
		Globals.sitePorts.add(port5);
	}
}
