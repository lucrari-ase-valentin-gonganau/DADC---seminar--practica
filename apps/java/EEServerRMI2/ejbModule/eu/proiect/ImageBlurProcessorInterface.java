package eu.proiect;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ImageBlurProcessorInterface extends Remote {
	ImageProcessingResult processIt(byte[] imageBytes, String typeImage, String uploadId) 
        throws RemoteException;
}