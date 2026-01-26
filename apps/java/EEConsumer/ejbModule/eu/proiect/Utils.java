package eu.proiect;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;


import javax.imageio.ImageIO;

public class Utils {

	public static String detectImageFormat(byte[] imageBytes) {
		if(imageBytes.length < 4) return null;
		
	    if(imageBytes[0] == 0x42 && imageBytes[1] == 0x4D) {
	        return "bmp";
	    }
	    
	    if(imageBytes[0] == (byte)0x89 && imageBytes[1] == 0x50 && 
	       imageBytes[2] == 0x4E && imageBytes[3] == 0x47) {
	        return "png";
	    }

	    if(imageBytes[0] == (byte)0xFF && imageBytes[1] == (byte)0xD8) {
	        return "jpg";
	    }
	
	    if(imageBytes[0] == 0x47 && imageBytes[1] == 0x49 && imageBytes[2] == 0x46) {
	        return "gif";
	    }
	    
	    return null;		
	}
	
	
    public static byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
    
    



    public static byte[] combineImages(byte[] leftBytes, byte[] rightBytes, String format) throws IOException {
        BufferedImage leftImage = ImageIO.read(new ByteArrayInputStream(leftBytes));
        BufferedImage rightImage = ImageIO.read(new ByteArrayInputStream(rightBytes));
        
        if (leftImage == null || rightImage == null) {
            throw new IOException("Failed to read processed image halves");
        }
        
        int totalWidth = leftImage.getWidth() + rightImage.getWidth();
        int maxHeight = Math.max(leftImage.getHeight(), rightImage.getHeight());
        
        System.out.println("Combining: left=" + leftImage.getWidth() + "x" + leftImage.getHeight() + 
                           ", right=" + rightImage.getWidth() + "x" + rightImage.getHeight());
        

        BufferedImage combined = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = combined.createGraphics();
        
  
        g.drawImage(leftImage, 0, 0, null);
        
        g.drawImage(rightImage, leftImage.getWidth(), 0, null);
        
        g.dispose();
        
        return imageToBytes(combined, format);
    }
	
}
