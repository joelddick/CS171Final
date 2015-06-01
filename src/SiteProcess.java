import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class SiteProcess{
	private CommThread myComm;
	private boolean failing = false;
	
	public SiteProcess(){
		myComm = new CommThread();
		myComm.start();
		try{
			BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));
			while (true){
				String input = cin.readLine();
				try{
					processInput(input);
				}
				catch (InterruptedException e){
					System.out.println(e.toString());
				}
			}
		}
		catch(IOException e){
			System.out.println(e.toString());
		}
	}
	
	private void processInput(String input) throws InterruptedException{
		if (input.length() < 4){
			return;
		}
		if (input.substring(0, 4).equals("Fail")){
			if(!failing){
				failing = true;
				myComm.wait();
			}
		}
		else if (input.substring(0, 7).equals("Restore")){
			if(failing){
				failing = false;
				myComm.notify();
			}
		}
	}
}