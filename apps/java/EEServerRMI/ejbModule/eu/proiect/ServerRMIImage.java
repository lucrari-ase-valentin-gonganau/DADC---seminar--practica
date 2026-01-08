package eu.proiect;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerRMIImage {
	static final String RMI_SERVER_PORT = "1099";
	
	public static void main(String[] args) {
	
		try {
		    String serverPort = new String(System.getenv().getOrDefault("RMI_SERVER_PORT", RMI_SERVER_PORT));
		    
		    Registry registery = LocateRegistry.createRegistry(Integer.parseInt(serverPort));
			
			
			ImageProcessor ip = new ImageProcessor();
			
			registery.rebind("ImageProcessorService", ip);
			
			System.out.println("SERVER RMI 1 - Zoom Image Started on port: "  + serverPort);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			

		}
		
	}
}
