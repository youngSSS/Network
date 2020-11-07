package peerToPeer;

import java.io.*;
import java.net.*;

//client means peer who wants to download file from here
public class Upload extends Thread{
	FileManager fileManager;
	Socket connectionSocket;
	long start;
	
	// Constructor
	public Upload (FileManager fileManager, Socket connectionSocket) {
		this.fileManager = fileManager;
		this.connectionSocket = connectionSocket;
		this.setName("Upload Thread-" + fileManager.GetUploadThreadNum());
	}
	
	public void run() {
		try {
			Thread.currentThread();
			while (!Thread.interrupted()) {
				String messageFromClient;
				int chunkIdx;
				
				fileManager.SetRunningThreadCnt();
				
				DataInputStream inFromClient = new DataInputStream(connectionSocket.getInputStream()); 
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				
				// Receive file name from client
				messageFromClient = inFromClient.readUTF();
				sleep(1000);
				
				// Case : 클라이언트가 원하는 파일을 서버가 가지고 있는 경우
				if (messageFromClient.equals(fileManager.fileName)) {
					outToClient.writeUTF(fileManager.chunkMap);
					System.out.println("[" + this.getName() + "]" + " Send chunk map to client");
					sleep(1000);
				}
				// Case : 클라이언트가 원하는 파일이 서버에 없는 경우
				else {
					System.out.println("[" + this.getName() + "]" + messageFromClient + " doesn't exist.");
					outToClient.writeUTF("No file");
					sleep(1000);
				}
				
				while (true) {					
					messageFromClient = inFromClient.readUTF();
					
					// messageFromClient가 file 이름과 동일한 경우 자신이 원한는 file chunk가 없다는 뜻으로 새롭게 갱신된 chunkMap을 전송하여준다.
					while ((messageFromClient).equals(fileManager.fileName)) {
						sleep(1000);
						start = System.currentTimeMillis();
						while (true) {
							if (((System.currentTimeMillis() - start) / 1000.0 > 5) || (fileManager.GetDownloadCnt() >= 3)) {
								outToClient.writeUTF(fileManager.chunkMap);
								sleep(1000);
								fileManager.ReleaseDownloadCnt();
								break;
							}
						}
						messageFromClient = inFromClient.readUTF();
					}
					
					System.out.println("[" + this.getName() + "]" + " Receive message from client : " + messageFromClient);
					sleep(1000);
					chunkIdx = Integer.parseInt(messageFromClient);
					outToClient.write(fileManager.fileChunks[chunkIdx]);
					System.out.println("[" + this.getName() + "]" + " Send " + chunkIdx + "th file chunk");
					sleep(1000);
					
				}
				
			}
		}
		catch (IOException e) {
			this.interrupt();
			System.out.println("[" + this.getName() + "]" + " This thread's work is done. Kill this thread.");
			System.out.println("Number of runnig upload thread : " + fileManager.GetRunningThreadCnt());
		} 
		catch (InterruptedException e) {
			System.out.println("[" + this.getName() + "]" + " Sleep error");
		}
	}
}
