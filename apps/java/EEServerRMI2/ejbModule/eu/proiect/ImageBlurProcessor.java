package eu.proiect;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.imageio.ImageIO;



public class ImageBlurProcessor extends UnicastRemoteObject implements ImageBlurProcessorInterface {
	private static final long serialVersionUID = 1L;
	
	protected ImageBlurProcessor() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}


	@Override
	public ImageProcessingResult processIt(byte[] imageBytes, String typeImage, String uploadId) {
    		
		System.out.println("Start image processing on RMI SERVER 2");
		System.out.println("Upload ID: " + uploadId);
		System.out.println("Received image bytes: " + imageBytes.length);
		System.out.println("Type image" + typeImage);
	
		try {
			if(imageBytes == null || imageBytes.length == 0) {
		        throw new IOException("Empty image received");
		    }
	

			ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
			BufferedImage originalImage = ImageIO.read(bais);
			BufferedImage blurredImage = applyBlur(originalImage);
	

	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
  
			ImageIO.write(blurredImage, typeImage, baos);

			return new ImageProcessingResult(uploadId, baos.toByteArray());
			
        } catch (IOException e) {
			e.printStackTrace();
			return new ImageProcessingResult(uploadId, e.getMessage());
		}
	}
	
	
	private BufferedImage applyBlur(BufferedImage image) {
	    int width = image.getWidth();
	    int height = image.getHeight();
	    
	    BufferedImage blurred = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); 
	    
	    int radius = 3;
	    
	    for (int y = 0; y < height; y++) {
	        for (int x = 0; x < width; x++) {
	            int red = 0, green = 0, blue = 0;
	            int count = 0;
	            

	            for (int dy = -radius; dy <= radius; dy++) {
	                for (int dx = -radius; dx <= radius; dx++) {
	                    int nx = x + dx;
	                    int ny = y + dy;
	                    
	                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
	                        int rgb = image.getRGB(nx, ny);
	                        red += (rgb >> 16) & 0xFF;
	                        green += (rgb >> 8) & 0xFF;
	                        blue += rgb & 0xFF;
	                        count++;
	                    }
	                }
	            } 
	            

	            red /= count;
	            green /= count;
	            blue /= count;
	            
	            int newRgb = (red << 16) | (green << 8) | blue;
	            blurred.setRGB(x, y, newRgb);
	        }
	    }
	    
	    return blurred;
	}

}
