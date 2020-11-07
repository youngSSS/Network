package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
   
	public static void main(String[] args) {
		try {
			Scanner S = new Scanner(System.in);
			String fileName, filePath;
			int port, chunkNum, chunkSize, readCnt;
			File file;
			InetAddress myIp = InetAddress.getLocalHost();
	
			System.out.println("************* Server *************");
			System.out.println("Server IP - " + myIp.getHostAddress());
			System.out.print("File name : ");
			fileName = S.next();
			filePath = System.getProperty("user.dir") + "\\";
   
			file = new File(filePath + fileName);
         
			FileInputStream fi = new FileInputStream(file);
			BufferedInputStream is = new BufferedInputStream(fi);
           
			chunkNum = (int) file.length() / 10240 + 1;
			chunkSize = 1024 * 10;
			readCnt = 0;
           
			byte[][] fileFragments = new byte[chunkNum][];
			byte[] tempBuffer = new byte[chunkSize];
           
			for (int i = 0; i < fileFragments.length; i++) {
				readCnt = is.read(tempBuffer, 0, chunkSize);
				fileFragments[i] = new byte[readCnt];
				fileFragments[i] = Arrays.copyOf(tempBuffer, readCnt);
			}
           
			is.close();
			fi.close();
			
			System.out.print("Server port : ");
			port = S.nextInt();
			
			ServerSocket welcomeSocket = new ServerSocket(port);
     
			System.out.println("\n-> Waiting for connection");
			Socket connectionSocket = welcomeSocket.accept();
			System.out.println("-> Connected\n");
			OutputStream  outToClient = connectionSocket.getOutputStream();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 

			if (inFromClient.readLine().equals(fileName)) {
				for (int i = 0; i < fileFragments.length; i++) {
					outToClient.write(fileFragments[i]);
					System.out.println("Send " + i + "th file chunk");
				}
				System.out.println("\n-> Complete to send file");
			}
			else
				System.out.println("File that client wants doesn't exist\n");

			welcomeSocket.close();
			connectionSocket.close();
			outToClient.close();
			inFromClient.close();

			System.out.println("-> Disconnected");
		}
		catch (UnknownHostException e) {
			System.out.println("Exception : Unable to find host IP");
	    }
		catch (Exception e) {
			System.out.println("-> File doesn't exist in Server folder");
		}
   }
}