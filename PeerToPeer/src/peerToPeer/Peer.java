package peerToPeer;

import java.io.*;
import java.net.*;
import java.util.*;

public class Peer{

	public static void main(String[] args) throws IOException {
		Scanner S = new Scanner(System.in);
		String fileName, filePath = System.getProperty("user.dir") + "/";
		FileManager fileManager = new FileManager();
		InetAddress myIp = InetAddress.getLocalHost();
		String[] ipArr = new String[5];
		int[] portArr = new int[5];
		int userNum;
		File file;
		
		System.out.println("***************************** Start P2P *****************************");
		System.out.println(" ___________________________________________________________________");
		System.out.println("|                           < How to use >                          |");
		System.out.println("|-------------------------------------------------------------------|");
		System.out.println("|0. Set the configuration file (change local IP)                    |");
		System.out.println("|-------------------------------------------------------------------|");
		System.out.println("|1. Case : Seeder                                                   |");
		System.out.println("|-> Use 1 for user number                                           |");
		System.out.println("|-> Enter the file name in the folder where the current code exists |");
		System.out.println("|-------------------------------------------------------------------|");
		System.out.println("|2. Case : leecher                                                  |");
		System.out.println("|-> Use 2, 3, 4 or 5 for user number                                |");
		System.out.println("|-> Enter the file name that you want to download                   |");
		System.out.println("|-> Warning!  Do not use duplicate number for user number           |");
		System.out.println("|___________________________________________________________________|");
		System.out.println("\nIP - " + myIp.getHostAddress());
		System.out.print("Press user number : ");
		userNum = S.nextInt();
		System.out.print("File name : ");
		fileName = S.next();
		
		// Get user information
		BufferedReader is = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath + "configuration.txt"))));
		
		String text = is.readLine();
		for (int i = 0; i < 4; i++)
			text += " " + is.readLine();       
		is.close();
		
		StringTokenizer token = new StringTokenizer(text, " ", false);
		
		for (int i = 0; i < 5; i++) {
			ipArr[i] = token.nextToken();
			portArr[i] = Integer.parseInt(token.nextToken());
		}
				
		// Case : peer is Seeder
		if (userNum == 1) {
			file = new File(filePath + fileName);
			
			fileManager.setInfo(fileName, userNum);
			fileManager.split(file);
			
			fileManager.isSeeder = true;
		}
		
		// Case : peer is leecher
		else {
			fileManager.setInfo(fileName, userNum);
			
			// 0�� �ε����� ������ �ڽ��� ������ �ٲپ� �ش�
			int tempPort = portArr[userNum - 1];
			String tempIp = ipArr[userNum - 1];
			
			portArr[userNum - 1] = portArr[0];
			ipArr[userNum - 1] = ipArr[0];
			
			portArr[0] = tempPort;
			ipArr[0] = tempIp;			
		}
		
		System.out.println();
		
		if (!fileManager.isSeeder) {
			Download download_1 = new Download(fileManager, ipArr, portArr);
			Download download_2 = new Download(fileManager, ipArr, portArr);
			Download download_3 = new Download(fileManager, ipArr, portArr);
			download_1.start();
			download_2.start();
			download_3.start();
		}
		
		ServerSocket welcomeSocket = new ServerSocket(portArr[0]);
		while(true) {
			Socket connectionSocket = welcomeSocket.accept();
			Upload upload = new Upload(fileManager, connectionSocket);
			upload.start();
		}
	}
}
