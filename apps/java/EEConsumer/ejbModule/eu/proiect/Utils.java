package eu.proiect;

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
	
}
