package eu.proiect;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

class ImageListener implements MessageListener {
	static final String RMI_SERVER_PORT_ZOOM = "1099";
	static final String RMI_SERVER_IP_ZOOM = "localhost";
	
	static final String RMI_SERVER_PORT_BLUR = "1100";
	static final String RMI_SERVER_IP_BLUR = "localhost";
	
    @Override
    public void onMessage(Message message) {

        byte[] imageData = null;
        
        try {
            if (message instanceof BytesMessage) {
                BytesMessage byteMsg = (BytesMessage) message;
        
                long length = byteMsg.getBodyLength();
                if (length > Integer.MAX_VALUE) {
                    throw new JMSException("The message is big to be processed.");
                }
     
                imageData = new byte[(int) length];
                byteMsg.readBytes(imageData);
                
                int x = byteMsg.getIntProperty("x");
                int y = byteMsg.getIntProperty("y");
                int w = byteMsg.getIntProperty("w");
                int h = byteMsg.getIntProperty("h");
                
                String uploadId = byteMsg.getStringProperty("uploadId");
                
                System.out.println("I received a binary message with the size of " + length + " bytes");
                System.out.println("Cords zoom: (" + x + "," + y + ") -> (" + w + "," + h + ")");

                processImageViaRMI(imageData, x, y, w, h, uploadId);
                
                
               
            } else if (message instanceof TextMessage) {
                TextMessage msg = (TextMessage) message;
                System.out.println("Received control message = " + msg.getText());
            } else {
                System.out.println("Type of message is unknown: " + message.getClass().getName());
            }
        } catch (JMSException jmse) {
            jmse.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    private void processImageViaRMI(byte[] imageBytes, int x, int y, int w, int h, String uploadId) throws IOException, SQLException {
    	System.out.println("Loading image of " + imageBytes.length + " bytes for processing via RMI");
        
        int port = Integer.parseInt(System.getenv().getOrDefault("RMI_SERVER_PORT_ZOOM", RMI_SERVER_PORT_ZOOM));
        String addressIp = System.getenv().getOrDefault("RMI_SERVER_IP_ZOOM", RMI_SERVER_IP_ZOOM);
        
        try {
        	Registry registry = LocateRegistry.getRegistry(addressIp, port);
        	
        	ImageProcessorInterface remote = (ImageProcessorInterface) registry.lookup("ImageProcessorService");
        	
        	if(imageBytes == null || imageBytes.length == 0) {
        		throw new IOException("Empty image received");
        	}
        	
  
        	String typeImage = Utils.detectImageFormat(imageBytes);
        	
        	if(typeImage == null) {
        		throw new IOException("That type of image is not supported!");
        	}
        	
        	System.out.println("The type of image is " + typeImage);
        	ImageProcessingResult raspuns = remote.processIt(imageBytes, typeImage, x, y, w, h, uploadId);
        	
        	Boolean nextStep = false;
        	long idInserted = -1;
        	
        	if(raspuns.getErrorMessage() == null && raspuns.getImage() != null) {
        		idInserted = db.saveAsBlob(imageBytes, raspuns.getImage(), typeImage);
        		System.out.println("The image was saved in database, and de ID is: " + idInserted);
        		System.out.println("The answer received from RMI SERVER: " + raspuns.getStatus());
        		Notification.notifyApp(uploadId, idInserted);
        		nextStep = true;
        	}
        	
        	
        	if (nextStep) {
        		System.out.println("Start RMI2 to put some blur on this zoomed image");
        		ImageProcessingResult raspuns2 =  processImageViaRMI2(raspuns.getImage(), typeImage, uploadId, idInserted);
        		
        		
        		if(raspuns2.getErrorMessage() == null && raspuns2.getImage() != null) {
        			
        			db.addBluredImageInDatabase(idInserted, raspuns2.getImage());
            		// notify again the application we have something new :)
            		
        			Notification.notifyApp(uploadId, idInserted);	
        		} else {
        			System.err.println("RMI 2 is out: " + raspuns2.getErrorMessage());
        		}
        		
        		
        	}
        	
        	System.out.println("Finished the job of consumer ....");
        	
        	
        	
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
    }
    
    
    private ImageProcessingResult processImageViaRMI2(byte[] imageBytes, String typeImage, String uploadId, long idInserted) {
    	System.out.println("Loading image of " + imageBytes.length + " bytes for processing via RMI 2");
        
        int port = Integer.parseInt(System.getenv().getOrDefault("RMI_SERVER_PORT_BLUR", RMI_SERVER_PORT_BLUR));
        String addressIp = System.getenv().getOrDefault("RMI_SERVER_IP_BLUR", RMI_SERVER_IP_BLUR);
        
    	Registry registry;
		try {
			registry = LocateRegistry.getRegistry(addressIp, port);
			ImageBlurProcessorInterface remote = (ImageBlurProcessorInterface) registry.lookup("ImageBlurProcessorService");
			
			return remote.processIt(imageBytes, typeImage, uploadId);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return null;
    	
    }
    
}
