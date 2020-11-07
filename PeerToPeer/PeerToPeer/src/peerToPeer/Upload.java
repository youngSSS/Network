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
				
				// Case : Ŭ���̾�Ʈ�� ���ϴ� ������ ������ ������ �ִ� ���
				if (messageFromClient.equals(fileManager.fileName)) {
					outToClient.writeUTF(fileManager.chunkMap);
					System.out.println("[" + this.getName() + "]" + " Send chunk map to client");
					sleep(1000);
				}
				// Case : Ŭ���̾�Ʈ�� ���ϴ� ������ ������ ���� ���
				else {
					System.out.println("[" + this.getName() + "]" + messageFromClient + " doesn't exist.");
					outToClient.writeUTF("No file");
					sleep(1000);
				}
				
				while (true) {					
					messageFromClient = inFromClient.readUTF();
					
					// messageFromClient�� file �̸��� ������ ��� �ڽ��� ���Ѵ� file chunk�� ���ٴ� ������ ���Ӱ� ���ŵ� chunkMap�� �����Ͽ��ش�.
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
