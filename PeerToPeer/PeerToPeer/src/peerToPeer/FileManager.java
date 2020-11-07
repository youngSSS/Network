package peerToPeer;

import java.io.*;
import java.util.*;

public class FileManager {
	String fileName, chunkMap = "";
	boolean isSeeder, isFirst, isLast;
	byte[][] fileChunks;
	int userNum, downloadCnt = 0, downloadThreadCnt = 0, UploadThreadCnt = 0, runnigThreadCnt = 0;
	String Using = "00000";
	
	
	public FileManager() {
		isSeeder = false;
		isFirst = true;
		isLast = true;
	}
	
	public void setInfo(String inputFileName, int userNum) {
		this.fileName = inputFileName;
		this.userNum = userNum;
	}
	
	// ****************** APIs for download ******************
	public synchronized int GetDownloadThreadNum() {
		downloadThreadCnt++;
		return downloadThreadCnt;
	}
	
	public synchronized boolean GetIsLast() {
		if (isLast) {
			isLast = false;
			return true;
		}
		else 
			return false;
		
	}
	
	public synchronized void SetDownloadCnt() {
		downloadCnt++;
	}
	
	public synchronized void SetUsing(int index, char changeNum) {
		if (index == Using.length() - 1)
			Using = Using.substring(0, index) + changeNum;
		else
			Using = Using.substring(0, index) + changeNum + Using.substring(index + 1);
	}
	
	public synchronized void SetFileChunkAndChunkMap(byte[] tempBuffer, int size, int index) {
		fileChunks[index] = new byte[size];
		fileChunks[index] = Arrays.copyOf(tempBuffer, size);
		
		if (index == chunkMap.length() - 1)
			chunkMap = chunkMap.substring(0, index) + "1";
		else
			chunkMap = chunkMap.substring(0, index) + "1" + chunkMap.substring(index + 1);
	}
	
	public synchronized void SetSize(String serverChunkMap) {
		if (isFirst) {
			isFirst = false;
			fileChunks = new byte[serverChunkMap.length()][];
			
			for (int i = 0; i < serverChunkMap.length(); i++) {
				chunkMap += '0';
			}
		}
	}
	
	public void ReleasePeer(int index) {
		if (0 < index && index < 5)
			SetUsing(index, '0');
	}
	
	public synchronized int SelectPeer() {
		int selectedNeighbor;
		while (true) {
			selectedNeighbor = (int) (Math.random() * 4) + 1;
			if (Using.charAt(selectedNeighbor) == '0')
				break;
		}
		SetUsing(selectedNeighbor, '1');
		return selectedNeighbor;
	}
	
	public synchronized String GetChunkIdx(String serverChunkMap) {
		Random random = new Random();
		ArrayList<Integer> needs = new ArrayList<>();
		int r = -1;
				
		// If server has chunk which I need, put in to needs.
		for (int j = 0; j < chunkMap.length(); j++) {
			if (chunkMap.charAt(j) == '0' && serverChunkMap.charAt(j) == '1')
				needs.add(j);
		}
		
		// Case : server has a chunk which I need. return chunk index.
		if (needs.size() != 0) {
			r = random.nextInt(needs.size());
			return Integer.toString(needs.get(r));
		}
		
		// Case : server doesn't have a chunk which I need. return -1.
		return Integer.toString(r);
	}
	
	public boolean isComplete() {
		// 자신의 chunk map 중 0이 있으면 count++
		int isComplete = 0;
		for (int i = 0; i < chunkMap.length(); i++) {
			if (chunkMap.charAt(i) == '0') 
				isComplete++;
		}
		
		// Case : 청크를 모두 얻은 경우
		if (isComplete == 0 && chunkMap.length() != 0) {
			isSeeder = true;
			return true;
		}
		
		// Case : 청크를 모두 얻지 못한 경우
		else return false;
	}
	
	
	// ****************** APIs for upload ******************
	// Split file to 10KB and set chunk map
	public void split(File file) throws IOException {
		FileInputStream fi = new FileInputStream(file);
		BufferedInputStream is = new BufferedInputStream(fi);
       
		int chunkNum = (int) file.length() / 10240 + 1;
		int chunkSize = 1024 * 10;
		int readCnt = 0;
       
		fileChunks = new byte[chunkNum][];
		byte[] tempBuffer = new byte[chunkSize];
       
		for (int i = 0; i < fileChunks.length; i++) {
			readCnt = is.read(tempBuffer, 0, chunkSize);
			fileChunks[i] = new byte[readCnt];
			fileChunks[i] = Arrays.copyOf(tempBuffer, readCnt);
			chunkMap += '1';
		}
       
		is.close();
		fi.close();
	}
	
	public synchronized int GetDownloadCnt() {
		return downloadCnt;
	}
	
	public synchronized void ReleaseDownloadCnt() {
		downloadCnt = 0;
	}
	
	public synchronized int GetUploadThreadNum() {
		UploadThreadCnt++;
		return UploadThreadCnt;
	}
	
	public synchronized void SetRunningThreadCnt() {
		runnigThreadCnt++;
	}
	
	public synchronized int GetRunningThreadCnt() {
		return --runnigThreadCnt;
	}
}
