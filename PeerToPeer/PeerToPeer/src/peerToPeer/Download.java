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
		String filePath = System.getProperty("user.dir") + "\\user_" + fileManager.userNum + "\\";
		Socket clientSocket;
		boolean keepConnect = false;
		long start = 0;
		int numOfDownload = 0;
		
		while (true) {
			try {
				// Case : leecher가 모든 chunk를 모아 seeder가 되었을 때 더이상 download를 진행하지 않는다.
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
					
				// 선택된 peer에서의 다운로드 횟수가 3번 이하일때만 실행된다
				while (numOfDownload < 3) {
					// 제일 처음 connection이므로 chunk map을 받아오게 된다.
					if (isFirst) {
						outToServer.writeUTF(fileManager.fileName);
						System.out.println("[" + this.getName() + "]" + " Send message requesting chunk map");
						sleep(1000);
						messageFromServer = inFromServer.readUTF();						
						System.out.println("[" + this.getName() + "]" + " Receive chunk map from server");
						sleep(1000);
						
						// Case : 피어가 내가 원하는 파일을 가지고 있지 않은 경우 또는 연결된 피어가 아직 청크맵을 형성하지 못한 경우 연결을 끊는다
						if (messageFromServer.equals("No file") || messageFromServer.equals("")) {
							System.out.println("[" + this.getName() + "]" + " No file in server");
							break;
						}						
						else {
							serverChunkMap = messageFromServer;
							
							// file chunk를 처음 받아오는 경우 chunkMap과 fileChunks의 크기가 초기화 알맞게 되어 있지 않으므로 설정해준다.
							if (fileManager.isFirst)
								fileManager.SetSize(serverChunkMap);
							
							// 이후에는 특별한 일이 없는 이상 지금 연결된 피어로 부터 더이상 chunk map을 받아 올 필요가 없으므로 isFisrt를 false로 설정
							isFirst = false;
						}
					}
					
					// chunk map을 받아 온 뒤 부터의 송수신
					else {
						// chunk map에서 받아올 chunk의 인덱스를 반환하는 GetFileIdx()
						// 필요한 chunk가 존재하지 않는다면 -1을 return.
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
							// 내가 필요한 청크의 인덱스를 전송
							outToServer.writeUTF(inFromUser);
							System.out.println("[" + this.getName() + "]" + " Send need chunk index");
							sleep(1000);
							
							
							// 피어에서 보내준 file chunk를 chunkFromServer에 저장
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
				// 특정 피어와 연결이 끊어진 경우 다른 피어의 chunk map을 새로 받아와야 하므로 isFirst를 true로 변경
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
