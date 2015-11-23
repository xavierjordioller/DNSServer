package dns;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Reply {
	
	private byte[] message;
	private byte[] domainName;
	private byte[] QName;
	private byte[] queryId;
	private String domainToSearch;
	
	private InetAddress senderIP;
	private int senderPort;
	private short anCount;
	
	private ArrayList<String> listIp;
	private short queryIdSh;
	private Integer queryIdInt;
	
	public byte[] getMessage() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message;
	}

	public byte[] getDomainName() {
		return domainName;
	}

	public void setDomainName(byte[] domainName) {
		this.domainName = domainName;
	}

	public byte[] getQName() {
		return QName;
	}

	public void setQName(byte[] qName) {
		QName = qName;
	}

	public Integer getQueryId() {
		return queryIdInt;
	}

	public void setQueryId(byte[] queryId) {
		this.queryId = queryId;
	}

	public String getDomainToSearch() {
		return domainToSearch;
	}

	public InetAddress getSenderIP() {
		return senderIP;
	}

	public void setSenderIP(InetAddress senderIP) {
		this.senderIP = senderIP;
	}

	public int getSenderPort() {
		return senderPort;
	}

	public void setSenderPort(int senderPort) {
		this.senderPort = senderPort;
	}

	public short getAnCount() {
		return anCount;
	}

	public void setAnCount(short anCount) {
		this.anCount = anCount;
	}

	public ArrayList<String> getListIp() {
		return listIp;
	}

	public void setListIp() {
		listIp = new ArrayList<String>();
		
		int offset = 12 + QName.length + 14;
		for (int i = 0; i < anCount; i++){
			int RDLen;
			byte[] RDLenB = new byte[2];
			RDLenB[0] = message[offset];
			RDLenB[0] = message[offset+1];
			offset = offset + 2;
			
			ByteBuffer wrapped = ByteBuffer.wrap(RDLenB);
			RDLen = wrapped.getShort();
			
			byte[] ip = new byte[4];
			if (RDLen == 4) {
				for(int j = 0; j < 4; j++){
					ip[j] = message[offset + j];
				}
			}
			offset = offset + RDLen;
			
			listIp.add(IpByteToString(ip));
		}
	}

	private String IpByteToString(byte[] ip) {
		String ipStr = "";
		for (int i = 0; i < ip.length; i++){
			ipStr = ipStr + String.valueOf(ip[i]) + ".";
		}
		ipStr = ipStr.substring(0,ipStr.length()-1);
		return ipStr;
	}

	public Reply(byte[] message) {
		this.message = message;
	}

	public void setDomainName() {
		int j = 0;
		int QNameLen = 0;
		while(message[12 + j] != 0x00) {
			QNameLen++;
			j++;
		}
		QNameLen++;
		
		QName = new byte[QNameLen];
		domainName = new byte[QNameLen];
		j = 0;
		QName[j] = message[12+j];
		domainName[j] = 0x00;
		while(message[12 + j] != 0x00) {
			int i = 1;
			for(; i <= (int) message[12+j]; i++) {
				QName[j+i] = message[12+j+i];
				domainName[j + i] = message[12+j+i];  
			}
			j = j + i;
			QName[j] = message[12+j];
			domainName[j] = 0x2E; //point
		}
		domainName[j] = 0x00;
		
		domainToSearch = new String(domainName);
		domainToSearch = domainToSearch.substring(1,domainToSearch.length()-1);
	}
	
	/*
	public void setDomainName() {
		// On trouve la taille de domainName et du QName complet
		int domainNameLen = 0;
		int QNameLen = 0;
		
		int j = 0;
		boolean start = false;
		while(message[12 + j] != 0x00) {
			if (message[12 + j] == 0x06) {
				start = true;
				QNameLen++;
			}
			else {
				QNameLen++;
				if (start) {
					domainNameLen++;
				}
			}			
			j++;
		}
		// On compte 0x00 dans le QName complet
		QNameLen++;
		
		// Initialisation
		this.QName = new byte[QNameLen];
		this.domainName = new byte[domainNameLen];
		
		// Stockage du Domain Name
		int startInd = QNameLen - domainNameLen - 1;
		int finishInd = QNameLen - 1;
		
		for (int i = 0; i < QNameLen; i++) {
			if (i >= startInd && i < finishInd) {
				if (message[12 + i] == 0x02) {
					domainName[i-startInd] = 0x2E;
				}
				else {
					domainName[i-startInd] = message[12 + i];
				}
			}
			QName[i] = message[12 + i];
		}
		
	}
*/
	
	public void setQueryId() {
		queryId = new byte[2];
		this.queryId[0] = this.message[0];
		this.queryId[1] = this.message[1];
		ByteBuffer wrapped = ByteBuffer.wrap(this.queryId); // big-endian by default
		this.queryIdSh = wrapped.getShort();
		this.queryIdInt = Integer.valueOf(this.queryIdSh);
	}

	public void setAncount() {
		byte[] anCountB = new byte[2];
		anCountB[0] = message[6];
		anCountB[1] = message[7];
		
		ByteBuffer wrapped = ByteBuffer.wrap(anCountB);
		this.anCount = wrapped.getShort();
	}

}
