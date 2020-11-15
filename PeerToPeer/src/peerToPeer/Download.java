package peerToPeer;

import java.io.*;
import java.net.*;

public class Download extends Thread {
	FileManager fileManager;
	String[] ipArr;
	int[] portArr;
	boolean isFirst;
	int selectedNeighbor = -1, selectedNeighborRightBefore = -1;
	
	// Constructor 
	public Download (FileManager fileManager, String[] ipArr, int[] portArr) {
		this.fileManager = fileManager;
		this.ipArr = ipArr;
		this.portArr = portArr;
		isFirst = true;
		this.setName("Download Thread-" + fileManager.GetDownloadThreadNum());
	}
	
	public void run() {
		String messageFromServer, serverChunkMap = "", inFromUser;
		String filePath = System.getProperty("user.dir") + "/user_" + fileManager.userNum + "/";
		Socket clientSocket;
		boolean keepConnect = false;
		long start = 0;
		int numOfDownload = 0;
		
		while (true) {
			try {
				// Case : leecher�� ��� chunk�� ��� seeder�� �Ǿ��� �� ���̻� download�� �������� �ʴ´�.
				if (fileManager.isComplete()) {
					fileManager.isSeeder = true;
					FileOutputStream fw = new FileOutputStream(filePath + fileManager.fileName);
					for (int i = 0; i < fileManager.chunkMap.length(); i++)
						fw.write(fileManager.fileChunks[i]);
					fw.close();
					
					if (fileManager.GetIsLast())
						System.out.println("[" + this.getName() + "]" + "\n-> Download complete");
					break;
				}
				
				do {
					fileManager.ReleasePeer(selectedNeighbor);
					selectedNeighbor = fileManager.SelectPeer();
				} while (selectedNeighbor == selectedNeighborRightBefore);
				selectedNeighborRightBefore = selectedNeighbor;
				numOfDownload = 0;
				
				clientSocket = new Socket(ipArr[selectedNeighbor], portArr[selectedNeighbor]);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
					
				// ���õ� peer������ �ٿ�ε� Ƚ���� 3�� �����϶��� ����ȴ�
				while (numOfDownload < 3) {
					// ���� ó�� connection�̹Ƿ� chunk map�� �޾ƿ��� �ȴ�.
					if (isFirst) {
						outToServer.writeUTF(fileManager.fileName);
						System.out.println("[" + this.getName() + "]" + " Send message requesting chunk map");
						sleep(1000);
						messageFromServer = inFromServer.readUTF();						
						System.out.println("[" + this.getName() + "]" + " Receive chunk map from server");
						sleep(1000);
						
						// Case : �Ǿ ���� ���ϴ� ������ ������ ���� ���� ��� �Ǵ� ����� �Ǿ ���� ûũ���� �������� ���� ��� ������ ���´�
						if (messageFromServer.equals("No file") || messageFromServer.equals("")) {
							System.out.println("[" + this.getName() + "]" + " No file in server");
							break;
						}						
						else {
							serverChunkMap = messageFromServer;
							
							// file chunk�� ó�� �޾ƿ��� ��� chunkMap�� fileChunks�� ũ�Ⱑ �ʱ�ȭ �˸°� �Ǿ� ���� �����Ƿ� �������ش�.
							if (fileManager.isFirst)
								fileManager.SetSize(serverChunkMap);
							
							// ���Ŀ��� Ư���� ���� ���� �̻� ���� ����� �Ǿ�� ���� ���̻� chunk map�� �޾� �� �ʿ䰡 �����Ƿ� isFisrt�� false�� ����
							isFirst = false;
						}
					}
					
					// chunk map�� �޾� �� �� ������ �ۼ���
					else {
						// chunk map���� �޾ƿ� chunk�� �ε����� ��ȯ�ϴ� GetFileIdx()
						// �ʿ��� chunk�� �������� �ʴ´ٸ� -1�� return.
						inFromUser = fileManager.GetChunkIdx(serverChunkMap);
						
						// Case : server doesn't have chunk which I need
						if (inFromUser.equals("-1")) {
							System.out.println("[" + this.getName() + "]" + " Server doesn't have chunk which I need.");
							if (!keepConnect) {
								start = System.currentTimeMillis();
								keepConnect = true;
							}							
							else {
								System.out.println("[" + this.getName() + "]" + " It's been " + (System.currentTimeMillis() - start) / 1000 + "sec since I didn't get chunk");
								
								if ((System.currentTimeMillis() - start) / 1000 >= 10) {
									System.out.println("[" + this.getName() + "]" + " Timeout. Find new peer.");
									keepConnect = false;
									break;
								}								
								else {
									System.out.println("[" + this.getName() + "]" + " Waiting for updated chunk map");
									outToServer.writeUTF(fileManager.fileName);
									sleep(1000);
									serverChunkMap = inFromServer.readUTF();
									System.out.println("[" + this.getName() + "]" + " Receive updated chunk map from server");
									sleep(1000);
								}
							}
						}
						
						// Case : server has chunk which I need
						else {
							// ���� �ʿ��� ûũ�� �ε����� ����
							outToServer.writeUTF(inFromUser);
							System.out.println("[" + this.getName() + "]" + " Send need chunk index");
							sleep(1000);
							
							
							// �Ǿ�� ������ file chunk�� chunkFromServer�� ����
							byte[] tempBuffer = new byte[10240];
							int size = inFromServer.read(tempBuffer);
							System.out.println("[" + this.getName() + "]" + " Receive need chunk, size : " + size);
							sleep(1000);
							
							fileManager.SetFileChunkAndChunkMap(tempBuffer, size, Integer.parseInt(inFromUser));
							numOfDownload++;
							keepConnect = false;
							fileManager.SetDownloadCnt();
							
							System.out.println("[" + this.getName() + "]" + " Server : " + serverChunkMap);
							System.out.println("[" + this.getName() + "]" + " Client : " + fileManager.chunkMap);
							System.out.println("[" + this.getName() + "]" + " Download " + inFromUser + "th file chunk");
						}
					}
				}
				// Ư�� �Ǿ�� ������ ������ ��� �ٸ� �Ǿ��� chunk map�� ���� �޾ƿ;� �ϹǷ� isFirst�� true�� ����
				fileManager.ReleasePeer(selectedNeighbor);
				isFirst = true;
				clientSocket.close();
			}
			catch (FileNotFoundException e) {
				System.out.println("[" + this.getName() + "]" + " FileNotFoundException");
			}
			catch (IOException e) {
				fileManager.ReleasePeer(selectedNeighbor);
			} 
			catch (InterruptedException e) {
				System.out.println("[" + this.getName() + "]" + " InterruptedException");
			}
		}
		System.out.println("[" + this.getName() + "]" + " This thread's work is done. Kill this thread.");
	}	
}
