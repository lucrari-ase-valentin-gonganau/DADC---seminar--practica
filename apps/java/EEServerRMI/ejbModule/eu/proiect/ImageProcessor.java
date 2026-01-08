package eu.proiect;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.imageio.ImageIO;



public class ImageProcessor extends UnicastRemoteObject implements ImageProcessorInterface {
	private static final long serialVersionUID = 1L;
	
	protected ImageProcessor() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public ImageProcessingResult processIt(byte[] imageBytes, String typeImage, int x, int y, int w, int h, String uploadId) {
    		
		System.out.println("Start image processing on RMI SERVER 1");
		System.out.println("Upload ID: " + uploadId);
		System.out.println("Received image bytes: " + imageBytes.length);
		System.out.println("Type image" + typeImage);
		System.out.println("Coordinates: x=" + x + ", y=" + y + ", w=" + w + ", h=" + h);
		
		try {
			if(imageBytes == null || imageBytes.length == 0) {
		        throw new IOException("Empty image received");
		    }
	

			ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
			BufferedImage originalImage = ImageIO.read(bais);
	
	        
	        int startX = Math.max(0,  Math.min(x, originalImage.getWidth() - 1));
	        int startY = Math.max(0,  Math.min(y,  originalImage.getHeight() - 1));
	        int width = w;
	        int height = h;
	        
	        System.out.println("Zoom region: x=" + startX + ", y=" + startY + ", w=" + width + ", h=" + height);
	
	        BufferedImage zoomedImage = originalImage.getSubimage(startX, startY, width, height);
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
  
			ImageIO.write(zoomedImage, typeImage, baos);

			return new ImageProcessingResult(uploadId, baos.toByteArray());
			
        } catch (IOException e) {
			e.printStackTrace();
			return new ImageProcessingResult(uploadId, e.getMessage());
		}
	}

}
