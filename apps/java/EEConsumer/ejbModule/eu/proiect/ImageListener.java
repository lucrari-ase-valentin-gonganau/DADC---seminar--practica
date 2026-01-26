package eu.proiect;

import java.awt.image.BufferedImage;
import java.io.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

class ImageListener implements MessageListener {
	static final String RMI_SERVER_PORT_ZOOM = "1099";
	static final String RMI_SERVER_IP_ZOOM = "localhost";
	
	static final String RMI_SERVER_PORT_ZOOM2 = "1100";
	static final String RMI_SERVER_IP_ZOOM2 = "localhost";
	
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
                
                double zoom = byteMsg.getDoubleProperty("zoom");
                		
                
                String uploadId = byteMsg.getStringProperty("uploadId");
                
                System.out.println("I received a binary message with the size of " + length + " bytes");
                System.out.println("Zoom params received: (" + zoom + ")");

                processImageViaRMI(imageData, zoom, uploadId);
                
                
               
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


    private void processImageViaRMI(byte[] imageBytes, double zoomFactor, String uploadId) throws IOException, SQLException {
    	System.out.println("Loading image of " + imageBytes.length + " bytes for processing via RMI");
        

        
        try {
        	// is not necessary to check if that is a image or the format is BMP, we already know 
        	// that project accept only bmp format, so the consumer will check  those data for us
        	// we also know the zoom will be between 0.1 and 10
        	
        	BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
        	
        	if (originalImage == null) {
        		throw new IOException("Failed to read image");
        	}
        	
        	String typeImage = "bmp";
        	
        	
        	int imgWidth = originalImage.getWidth();
        	int imgHeight = originalImage.getHeight();
        	int halfWidth = imgWidth / 2;
        	
        	System.out.println("Original image size: " + imgWidth + "x" + imgHeight);
        	System.out.println("Splitting image into two halves at x=" + halfWidth);
        	
        	BufferedImage leftHalf = originalImage.getSubimage(0, 0,  halfWidth, imgHeight);
            BufferedImage rightHalf = originalImage.getSubimage(halfWidth, 0, imgWidth - halfWidth, imgHeight);
            
            byte[] leftHalfBytes = Utils.imageToBytes(leftHalf, typeImage);
            byte[] rightHalfBytes = Utils.imageToBytes(rightHalf, typeImage);
            
            System.out.println("Left half: " + leftHalfBytes.length + " bytes");
            System.out.println("Right half: " + rightHalfBytes.length + " bytes");
            
            
            // We have 2 servers rmi for this task 
            ExecutorService executor = Executors.newFixedThreadPool(2);
            
            Future<ImageProcessingResult> leftResult = executor.submit(() -> {
            	return processHalfOnRMIServer1(leftHalfBytes, typeImage, zoomFactor, uploadId);
            });
        	
            Future<ImageProcessingResult> rightResult = executor.submit(() -> {
            	return processHalfOnRMIServer2(rightHalfBytes, typeImage, zoomFactor, uploadId);
            });
            
            
            ImageProcessingResult leftProcessed = leftResult.get();
            ImageProcessingResult rightProcessed = rightResult.get();
            
            executor.shutdown(); // let's stop
            
            if (leftProcessed.getErrorMessage() != null) {
                throw new IOException("RMI Server 1 error: " + leftProcessed.getErrorMessage());
            }
            if (rightProcessed.getErrorMessage() != null) {
                throw new IOException("RMI Server 2 error: " + rightProcessed.getErrorMessage());
            }
            
            System.out.println("Both halves processed successfully. Combining...");
        	
            byte[] combinedImage = Utils.combineImages(
                    leftProcessed.getImage(), 
                    rightProcessed.getImage(), 
                    typeImage
                );
            
            System.out.println("Combined image size: " + combinedImage.length + " bytes");
            
            
            long idInserted = db.saveAsBlob(imageBytes, combinedImage, typeImage);
            System.out.println("The combined image was saved in database, ID: " + idInserted);
            
            Notification.notifyApp(uploadId, idInserted);
            
            System.out.println("Finished the job of consumer ....");
        	
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to process image via RMI", e);
        }
    }
    
    
    private ImageProcessingResult processHalfOnRMIServer1(byte[] imageBytes, String typeImage, 
            double zoomFactor, String uploadId) {
    	try {
			int port = Integer.parseInt(System.getenv().getOrDefault("RMI_SERVER_PORT_ZOOM", RMI_SERVER_PORT_ZOOM));
			String addressIp = System.getenv().getOrDefault("RMI_SERVER_IP_ZOOM", RMI_SERVER_IP_ZOOM);
			
			Registry registry = LocateRegistry.getRegistry(addressIp, port);
			ImageProcessorInterface remote = (ImageProcessorInterface) registry.lookup("ImageProcessorService");

			System.out.println("Processing LEFT half on RMI Server 1 with zoom factor: " + zoomFactor);
			return remote.processIt(imageBytes, typeImage, zoomFactor, uploadId);
		
			} catch (Exception e) {
				e.printStackTrace();
				ImageProcessingResult error = new ImageProcessingResult(uploadId, e.getMessage());
				return error;
			}
    }
    
    private ImageProcessingResult processHalfOnRMIServer2(byte[] imageBytes, String typeImage, 
            double zoomFactor, String uploadId) {
		try {
			int port = Integer.parseInt(System.getenv().getOrDefault("RMI_SERVER_PORT_ZOOM_2", "1100"));
			String addressIp = System.getenv().getOrDefault("RMI_SERVER_IP_ZOOM_2", "localhost");
			
			Registry registry = LocateRegistry.getRegistry(addressIp, port);
			ImageProcessorInterface remote = (ImageProcessorInterface) registry.lookup("ImageProcessorService");
			
			System.out.println("Processing RIGHT half on RMI Server 2 with zoom factor: " + zoomFactor);
			return remote.processIt(imageBytes, typeImage, zoomFactor, uploadId);
		
		} catch (Exception e) {
			e.printStackTrace();
			ImageProcessingResult error = new ImageProcessingResult(uploadId, e.getMessage());
			return error;
		}
	}
}
