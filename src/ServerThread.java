/* References:
 * http://www.ietf.org/rfc/rfc2821.txt
 * http://en.wikipedia.org/wiki/Simple_Mail_Transfer_Protocol
 * http://www.webbasedprogramming.com/JAVA-Developers-Guide/ch27.htm
 * http://www.cse.ust.hk/~golin/comp361_spr2004/project/smtp.html
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerThread extends Thread {
	private BufferedReader br;
	private PrintWriter pw;
	private Socket client;
	private boolean TerminationFlag;
	public boolean isTerminationFlag() {
		return TerminationFlag;
	}
	//Returns help message
	private String help() {
		return "This is a help message";
	}
	//Returns message for session quit
	private String quit() {
		return "221 Bye";
	}
	//Returns message for successful HELO (or EHLO) command
	private String helo() {
		return "250 Received HELO";
	}
	//Returns message for successful MAIL command
	private String mail() {
		return "250 Mail Received";
	}
	//Returns message for successful RCPT command
	private String rcpt() {
		return "250 Received RCPT";
	}
	//Returns message for successful DATA command, and tells users how to end data transmission
	private String data() {
		return "354 End data with <CR><LF>.<CR><LF>";
	}
	//Looks for <CR><LF>.<CR><LF> to signify the end of the DATA command
	boolean endOfData(String s) {
		if(s.equals(".")) {
			return true;
		}
		return false;
	}
	//Returns message for successful end to the data transmission
	private String dataReceived() {
		return "250 Ok";
	}
	//Returns message for a bad sequence of commands
	private String badSequence() {
		return "503 Bad sequence of commands";
	}
	//Returns message for an unrecognized command
	private String unrecognized() {
		return "500 Syntax error, command unrecognized";
	}
	public ServerThread(Socket client) {
		this.client = client;
		this.TerminationFlag = false;
	}
	public void run() {
		String temp = null;
		//Tracks the iteration state of the SMTP transaction
		String state = "START";
		//Collects the client's name
		@SuppressWarnings("unused")
		String clientName = null;
		//Collects the data from the SMTP message
		String dataFeed = null;
		try {
			br = new BufferedReader(new InputStreamReader(client.getInputStream()));
			pw = new PrintWriter(client.getOutputStream(),true);
			//Welcomes the user to the SMTP server
			pw.println("220 Hello, this is SMTP server.");
		} 
		catch (IOException e) {			
			System.err.println("I/O stream cannot be extracted");
			//-- to add additional processing
		}
		while(true) {
			try {
				temp = br.readLine();
				temp = temp.toUpperCase();
			} 
			catch (IOException e) {
				System.err.println("Readline() failed");
				//-- to add additional processing	
			}
			if(temp.equals("HELP")) {
				pw.println(help());
			}
			else if(temp.equals("QUIT")) {
				try {
					pw.println(quit());
					br.close();
					pw.close();
					client.close();
				} 
				catch (IOException e) {
					System.err.println("Clean up procedure failed");
					//-- to add additional processing
				}
				this.TerminationFlag = true;
				break;
			}
			//Recognizes HELO (or EHLO) command 
			else if(temp.startsWith("HELO") || (temp.startsWith("EHLO"))) {
				if (state == "START") {
					pw.println(helo());
					state = "HELO";
					//Collects client name
					clientName = temp.substring(5);
				}
				//Returns error if out of sequence
				else {
					pw.println(badSequence());
				}
			}
			//Recognizes MAIL command
			else if (temp.startsWith("MAIL")){
				if (state == "HELO"){
					pw.println(mail());
					state = "MAIL";
				}
				//Returns error if out of sequence
				else {
					pw.println(badSequence());
				}
			}
			//Recognizes RCPT command
			else if (temp.startsWith("RCPT")) {
				if (state == "MAIL" || state == "RCPT") {
					pw.println(rcpt());
					state = "RCPT";
				}
				//Returns error if out of sequence
				else {
					pw.println(badSequence());
				}
			}
			//Recognizes DATA command
			else if (temp.startsWith("DATA")) {
				if (state == "RCPT") {
					pw.println(data());
					state = "DATA";
					//Receives DATA text into dataFeed
					while(true) {
						try {
							dataFeed = br.readLine();
						} 
						catch (IOException e) {
							System.err.println("Readline() failed");
						}
						//Once <CR><LF>.<CR><LF> is entered, processes end of data sequence
						if(endOfData(dataFeed) == true) {
							pw.println(dataReceived()); 
						    state = "START";
						    break;
						}
					}
				}
				//Returns error if out of sequence
				else {
					pw.println(badSequence());
				}
			}
			//Returns error if command is unrecognized
			else {
				pw.println(unrecognized());
			}
		}
	}
}