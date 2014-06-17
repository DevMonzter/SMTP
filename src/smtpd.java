import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import javax.swing.text.html.ListView;

public class smtpd {

	public static void main(String[] args) {
		int port = 0;
		ArrayList<ServerThread> listOfThreads = new ArrayList<ServerThread>(); 
		if(args.length < 1) {  
			System.err.println("Need at least on command line argument");
			System.exit(1);
		}
		try {
			port = Integer.parseInt(args[0]);
		}
		catch(NumberFormatException ex) {
			System.err.println("First command-line argument must be an integer");
			System.exit(2);
		}
		ServerSocket server;
		Socket client;
		while(true) {
			//increases port number to outside of range of 1 to 1024, if requested port falls in that range
			if(port < 1025){
				port = 1025;
			}
			try {
				server = new ServerSocket(port);
				break;
			} 
			catch (IOException e) {			
				port++;
			}
		}
		System.out.println("Port number is " + port);
		try {
			server.setSoTimeout(5000);
		} 
		catch (SocketException e) {
			System.err.println("ServerSocket timeout setting failed: " + e);
		}
		while(true) {
			try {
				client = server.accept();
				//-- create new thread to serve new connection request
				System.out.println("New client from '" + client.getRemoteSocketAddress() + "' requested connection");
				ServerThread tempThread = new ServerThread(client);
				listOfThreads.add(tempThread);
				tempThread.start();
			} 
			catch (java.net.SocketTimeoutException e1) {
				/*
				for(ServerThread T : listOfThreads) {
					//-- Try this approach to implement iteration, it should throw an exception
				}
				*/
				for(int i=listOfThreads.size()-1; i>= 0; i--) {
					ServerThread termThread = listOfThreads.get(i);
					if(termThread.isTerminationFlag() == true) {
						try {
							termThread.join();
						} 
						catch (InterruptedException e2) {
							System.err.println("Joining of a thread failed: " + e2);
						}
						listOfThreads.remove(termThread);
						System.out.println("Thread has been successfully removed.");
					}
				}
			}
			catch (IOException e3) {
				System.err.println("Accept() method returned error: " + e3);
				//-- to add additional processing
			}			
		}
	}
}
