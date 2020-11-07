package Client;

import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
	
	public static void main(String[] args) {
		try {
			String fileName, FilePath = System.getProperty("user.dir") + "\\received_file\\", ip;
			int port;

			Scanner S = new Scanner(System.in);
 
			System.out.println("************* Client *************");
			System.out.print("File name : ");
			fileName = S.next();
			System.out.print("Server IP : ");
			ip = S.next();
			System.out.print("Server port : ");
			port = S.nextInt();
 
			Socket clientSocket = new Socket(ip, port);
			System.out.println("-> Success to connect with server\n");
			InputStream inFromServer = clientSocket.getInputStream();
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			FileOutputStream fw = new FileOutputStream(FilePath + fileName);
  
			outToServer.writeBytes(fileName + '\n');
  
			byte[] byteBuffer = new byte[1024 * 10];
			int size, cnt = 0;
			while (true) {
				size = inFromServer.read(byteBuffer);
				if (size != -1) {
					fw.write(byteBuffer, 0, size);
					System.out.println("Receive " + cnt++ + "th file chunk");
				}
				else
					break;
			}
  
			fw.close();
			clientSocket.close();
			outToServer.close();
		  inFromServer.close();
  
		  if (cnt != 0)
			  System.out.println("\n-> Download complete");
		  else 
			  System.out.println("\n-> \"" + fileName + "\" dosen't exist in Server");
		}
		catch (IOException e) {
			System.out.println("Exception : IO exception is occured");
		}
	}
}