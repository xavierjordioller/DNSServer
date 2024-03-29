package serverdns;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dns.Query;
import dns.Reply;

/**
 * Cette classe permet la reception d'un paquet UDP sur le port de reception
 * UDP/DNS. Elle analyse le paquet et extrait le hostname
 * 
 * Il s'agit d'un Thread qui ecoute en permanance pour ne pas affecter le
 * deroulement du programme
 * 
 * @author Max
 *
 */

public class UDPReceiver extends Thread {
	/**
	 * Les champs d'un Packet UDP 
	 * --------------------------
	 * En-tete (12 octects) 
	 * Question : l'adresse demande 
	 * Reponse : l'adresse IP
	 * Autorite :
	 * info sur le serveur d'autorite 
	 * Additionnel : information supplementaire
	 */

	/**
	 * Definition de l'En-tete d'un Packet UDP
	 * --------------------------------------- 
	 * Identifiant Parametres 
	 * QDcount
	 * Ancount
	 * NScount 
	 * ARcount
	 * 
	 * L'identifiant est un entier permettant d'identifier la requete. 
	 * parametres contient les champs suivant : 
	 * 		QR (1 bit) : indique si le message est une question (0) ou une reponse (1). 
	 * 		OPCODE (4 bits) : type de la requete (0000 pour une requete simple). 
	 * 		AA (1 bit) : le serveur qui a fourni la reponse a-t-il autorite sur le domaine? 
	 * 		TC (1 bit) : indique si le message est tronque.
	 *		RD (1 bit) : demande d'une requete recursive. 
	 * 		RA (1 bit) : indique que le serveur peut faire une demande recursive. 
	 *		UNUSED, AD, CD (1 bit chacun) : non utilises. 
	 * 		RCODE (4 bits) : code de retour.
	 *                       0 : OK, 1 : erreur sur le format de la requete,
	 *                       2: probleme du serveur, 3 : nom de domaine non trouve (valide seulement si AA), 
	 *                       4 : requete non supportee, 5 : le serveur refuse de repondre (raisons de s�ecurite ou autres).
	 * QDCount : nombre de questions. 
	 * ANCount, NSCount, ARCount : nombre d�entrees dans les champs �Reponse�, Autorite,  Additionnel.
	 */

	protected final static int BUF_SIZE = 1024;
	protected String SERVER_DNS = null;//serveur de redirection (ip)
	protected int portRedirect = 53; // port  de redirection (par defaut)
	protected int port; // port de r�ception
	private String adrIP = null; //bind ip d'ecoute
	private String DomainName = "none";
	private String DNSFile = null;
	private boolean RedirectionSeulement = false;
	
	private class ClientInfo { //quick container
		public InetAddress client_ip = null;
		public int client_port = 0;
	};
	private HashMap<Integer, ClientInfo> Clients = new HashMap<>();
	
	private boolean stop = false;

	
	public UDPReceiver() {
	}

	public UDPReceiver(String SERVER_DNS, int Port) {
		this.SERVER_DNS = SERVER_DNS;
		this.port = Port;
	}
	
	
	public void setport(int p) {
		this.port = p;
	}

	public void setRedirectionSeulement(boolean b) {
		this.RedirectionSeulement = b;
	}

	public String gethostNameFromPacket() {
		return DomainName;
	}

	public String getAdrIP() {
		return adrIP;
	}

	private void setAdrIP(String ip) {
		adrIP = ip;
	}

	public String getSERVER_DNS() {
		return SERVER_DNS;
	}

	public void setSERVER_DNS(String server_dns) {
		this.SERVER_DNS = server_dns;
	}



	public void setDNSFile(String filename) {
		DNSFile = filename;
	}
	
	String toBinary( byte[] bytes )	{
	    StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
	    for( int i = 0; i < Byte.SIZE * bytes.length; i++ )
	        sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
	    return sb.toString();
	}
	

	public void run() {
		try {
			DatagramSocket serveur = new DatagramSocket(this.port); // *Creation d'un socket UDP
			
			AnswerRecorder answerRecorder = new AnswerRecorder(DNSFile);
			QueryFinder queryFinder = new QueryFinder(this.DNSFile);
			
			// *Boucle infinie de recpetion
			while (!this.stop) {
				byte[] buff = new byte[0xFF];
				DatagramPacket paquetRecu = new DatagramPacket(buff,buff.length);
				System.out.println("Serveur DNS  "+serveur.getLocalAddress()+"  en attente sur le port: "+ serveur.getLocalPort());

				// *Reception d'un paquet UDP via le socket
				serveur.receive(paquetRecu);
				
				System.out.println("paquet recu du  "+paquetRecu.getAddress()+"  du port: "+ paquetRecu.getPort());
				

				// *Creation d'un DataInputStream ou ByteArrayInputStream pour
				// manipuler les bytes du paquet

				ByteArrayInputStream TabInputStream = new ByteArrayInputStream (paquetRecu.getData());
				
				System.out.println(buff.toString());
				
				
				// Sauvegarde de QR
				byte[] octet2 = new byte[1];
				octet2[0] = buff[2];
				String QR = toBinary(octet2).substring(0, 1);
				
				
				// ****** Dans le cas d'un paquet requete *****
				if ( QR.equals("0")) {
					Query query = new Query(buff);
					
					// *Lecture et sauvegarde du Query Domain name, a partir du 13 byte
					query.setDomainName();
					
					// *Sauvegarde de l'adresse, du port et de l'identifiant de la requete
					query.setSenderIP(paquetRecu.getAddress());
					query.setSenderPort(paquetRecu.getPort());
					query.setQueryId();

					// *Si le mode est redirection seulement
					if(this.RedirectionSeulement) {
						// *Rediriger le paquet vers le serveur DNS
						UDPSender UDPS = new UDPSender(this.SERVER_DNS,this.portRedirect,serveur);		
						UDPS.SendPacketNow(paquetRecu);
						
						/* STOCKAGE DU CLIENT */
						ClientInfo client = new ClientInfo();
						client.client_ip = query.getSenderIP();
						client.client_port = query.getSenderPort();
						
//						ByteBuffer wrapped = ByteBuffer.wrap(query.getQueryId());
//						Clients.put((int) wrapped.getShort(), client);
						Clients.put(query.getQueryId(), client);
						
					}
					else {
						// *Rechercher l'adresse IP associe au Query Domain name
						// dans le fichier de correspondance de ce serveur

					
						List<String> listIP = queryFinder.StartResearch(query.getDomainToSearch());

						// *Si la correspondance n'est pas trouvee
						if (listIP.isEmpty()) {
							// *Rediriger le paquet vers le serveur DNS
							UDPSender UDPS = new UDPSender(this.SERVER_DNS,this.portRedirect,serveur);
							UDPS.SendPacketNow(paquetRecu);
							
							// STOCKAGE DU CLIENT
							ClientInfo client = new ClientInfo();
							client.client_ip = query.getSenderIP();
							client.client_port = query.getSenderPort();
							
							//ByteBuffer wrapped = ByteBuffer.wrap(query.getQueryId());
							//Clients.put((int) wrapped.getShort(), client);
							Clients.put(query.getQueryId(), client);
						}
						// *Sinon	
						else {
							// *Creer le paquet de reponse a l'aide du UDPAnswerPaquetCreator
							UDPAnswerPacketCreator answerPacketCreator = UDPAnswerPacketCreator.getInstance();
							
							// *Placer ce paquet dans le socket
							byte[] answerBytes  = answerPacketCreator.CreateAnswerPacket(buff,listIP);
							DatagramPacket replyPacket = new DatagramPacket(answerBytes,answerBytes.length);
							
							// *Envoyer le paquet
							UDPSender UDPS = new UDPSender(query.getSenderIP(), query.getSenderPort(),serveur);
							UDPS.SendPacketNow(replyPacket);
						}
					}
				}
				
				
				// ****** Dans le cas d'un paquet reponse *****
				
				else { // !QR.equals("0")
					
					Reply reply = new Reply(buff);
					// Lecture de l'identifiant
					reply.setQueryId();
					
					ClientInfo client = Clients.get(reply.getQueryId());
					if (client == null) {
					//if (client.client_ip == null || client.client_port == 0) {
						System.out.println("Ce paquet et pas pour moi");
						continue;
					}
					
					// Lecture de ANCOUNT
					reply.setAncount();
					if (reply.getAnCount() == 0) {
						continue;
					} 
					
					
					// *Lecture du Query Domain name, a partir du 13 byte
					reply.setDomainName(); 

					
					// *Passe par dessus Type et Class
					
				
					// *Passe par dessus les premiers champs du ressource record
					// pour arriver au ressource data qui contient l'adresse IP associe
					//  au hostname (dans le fond saut de 16 bytes)
					reply.setListIp();
				
				
					// *Capture de ou des adresse(s) IP (ANCOUNT est le nombre
					// de reponses retournees)			
				
				
					// *Ajouter la ou les correspondance(s) dans le fichier DNS
					// si elles ne y sont pas deja
					List<String> savedIp =  queryFinder.StartResearch(reply.getDomainToSearch());
					
					for (int i = 0; i < reply.getListIp().size(); i++) {
						if (!savedIp.contains(reply.getListIp().get(i))) {
							answerRecorder.StartRecord(reply.getDomainToSearch(), reply.getListIp().get(i));
						}
					}
					
					
				
					// *Faire parvenir le paquet reponse au demandeur original,
					// ayant emis une requete avec cet identifiant
					UDPAnswerPacketCreator answerPacketCreator = UDPAnswerPacketCreator.getInstance();

					// *Placer ce paquet dans le socket
					byte[] answerBytes = answerPacketCreator.CreateAnswerPacket(buff, reply.getListIp());
					DatagramPacket replyPacket = new DatagramPacket(answerBytes,answerBytes.length);
					
					// *Envoyer le paquet
					UDPSender UDPS = new UDPSender(client.client_ip,client.client_port,serveur);
					UDPS.SendPacketNow(replyPacket);
				}
			}
//			serveur.close(); //closing server
		} catch (Exception e) {
			System.err.println("Probl�me � l'ex�cution :");
			e.printStackTrace(System.err);
		}
	}
}
