package dns;

import java.net.InetAddress;

public class Query {
	byte[] message;
	byte[] domainName;
	byte[] QName;
	byte[] queryId;
	String domainToSearch;
	
	InetAddress senderIP;
	int senderPort;
	
	public Query(byte[] message) {
		this.message = message;
		this.queryId = new byte[2];
	}
	
	
	public InetAddress getSenderIP() {
		return senderIP;
	}

	public int getSenderPort() {
		return senderPort;
	}

	public byte[] getMessage() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message;
	}

	public byte[] getDomainName() {
		return domainName;
	}

	public byte[] getQName() {
		return QName;
	}

	public void setQName(byte[] qName) {
		QName = qName;
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
		
	}*/

	public byte[] getQueryId() {
		return queryId;
	}


	public void setQueryId(byte[] queryId) {
		this.queryId = queryId;
	}


	public String getDomainToSearch() {
		return domainToSearch;
	}


	public void setDomainToSearch(String domainToSearch) {
		this.domainToSearch = domainToSearch;
	}


	public void setDomainName(byte[] domainName) {
		this.domainName = domainName;
	}


	public void setSenderIP(InetAddress senderIP) {
		this.senderIP = senderIP;
	}

	public void setSenderPort(int senderPort ) {
		this.senderPort = senderPort;
	}

	public void setQueryId() {
		this.queryId[0] = this.message[0];
		this.queryId[1] = this.message[1];
	}
	
	
	
	

}
