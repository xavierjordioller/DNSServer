package dns;

import java.net.InetAddress;

public class Query {
	byte[] message;
	byte[] domainName;
	byte[] QName;
	
	InetAddress senderIP;
	int senderPort;
	
	
	public InetAddress getSenderIP() {
		return senderIP;
	}

	public int getSenderPort() {
		return senderPort;
	}

	public Query(byte[] message) {
		this.message = message;
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

	public void setSenderIP(InetAddress senderIP) {
		this.senderIP = senderIP;
	}

	public void setSenderPort(int senderPort ) {
		this.senderPort = senderPort;
	}
	
	
	
	

}
