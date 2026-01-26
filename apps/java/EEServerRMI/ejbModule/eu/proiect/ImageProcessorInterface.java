package eu.proiect;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ImageProcessorInterface extends Remote {
	ImageProcessingResult processIt(byte[] imageBytes, String typeImage, double zoomFactory, String uploadId) 
        throws RemoteException;
}