package eu.proiect;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
	public ImageProcessingResult processIt(byte[] imageBytes, String typeImage, double zoomFactory, String uploadId) {
    		
        System.out.println("Start image processing on RMI SERVER");
        System.out.println("Upload ID: " + uploadId);
        System.out.println("Received image bytes: " + imageBytes.length);
        System.out.println("Type image: " + typeImage);
        System.out.println("Zoom factor: " + zoomFactory);
		
		try {
			if(imageBytes == null || imageBytes.length == 0) {
		        throw new IOException("Empty image received");
		    }
	
		    if(zoomFactory <= 0) {
                throw new IOException("Zoom factor must be positive (received: " + zoomFactory + ")");
            }
		    
			ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
			BufferedImage imageSegment = ImageIO.read(bais);
	
			if(imageSegment == null) {
			        throw new IOException("Failed to read image segment");
			}
			
			int segmentWidth = imageSegment.getWidth();
			int segmentHeight = imageSegment.getHeight();
	        
			
			System.out.println("Segment size BEFORE zoom: " + segmentWidth + "x" + segmentHeight);
	            
            int newWidth = (int) Math.round(segmentWidth * zoomFactory);
            int newHeight = (int) Math.round(segmentHeight * zoomFactory);
	        
            System.out.println("Segment size AFTER zoom: " + newWidth + "x" + newHeight);
            
            if(newWidth <= 0 || newHeight <= 0) {
                throw new IOException("Invalid dimensions after zoom: " + newWidth + "x" + newHeight);
            }
            
      
            BufferedImage zoomedSegment = zoomImageSegment(imageSegment, newWidth, newHeight);
            
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean written = ImageIO.write(zoomedSegment, typeImage, baos);
            
            if(!written) {
                throw new IOException("Failed to write image format: " + typeImage);
            }
            
            byte[] resultBytes = baos.toByteArray();
            System.out.println("Result segment bytes: " + resultBytes.length);
            System.out.println("=== RMI SERVER: Segment processing DONE ===\n");
            
            return new ImageProcessingResult(uploadId, resultBytes);
            
        } catch (IOException e) {
			e.printStackTrace();
			return new ImageProcessingResult(uploadId, e.getMessage());
		}
	}
	
	
	private BufferedImage zoomImageSegment(BufferedImage segment, int newWidth, int newHeight) {
		System.out.println("  -> Applying zoom transformation: " + 
                segment.getWidth() + "x" + segment.getHeight() + 
                " -> " + newWidth + "x" + newHeight);
		
		BufferedImage zoomedSegment = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		
        Graphics2D g2d = zoomedSegment.createGraphics();
        

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
   
        g2d.drawImage(segment, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return zoomedSegment;
	        
	      
	}

}
