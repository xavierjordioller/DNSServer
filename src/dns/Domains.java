package dns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Domains {
	ArrayList<String[]> listDomains;
		
	public Domains(Path path) {
		listDomains = new ArrayList<String[]>();
		try {
			InputStream in = Files.newInputStream(path);
			BufferedReader  reader = new BufferedReader(new InputStreamReader(in));
			String line = "";
			while ((line = reader.readLine()) != null) {
				String[] domain = line.split(";");
				listDomains.add(domain);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public ArrayList<String> searchIP(String domainName) {
		ArrayList<String> listIp = new ArrayList<String>();
		for (int i = 0; i < listDomains.size(); i++) {
			if (listDomains.get(i)[0].equals(domainName)) {
				for (int j = 1; j < listDomains.get(i).length; j++){
					listIp.add(listDomains.get(i)[j]);
				}
			}
		}
		return listIp;
	}
	
	

}
