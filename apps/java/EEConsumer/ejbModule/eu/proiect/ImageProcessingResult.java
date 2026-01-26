package eu.proiect;

import java.io.Serializable;

public class ImageProcessingResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String status;  
    private String uploadId;
    private byte[] image;
    private String errorMessage;  
    
    // Constructor for success
    public ImageProcessingResult(String uploadId, byte[] image) {
        this.status = "success";
        this.uploadId = uploadId;
        this.image = image;
    }
    
    // Constructor for error
    public ImageProcessingResult(String uploadId, String errorMessage) {
        this.status = "error";
        this.uploadId = uploadId;
        this.errorMessage = errorMessage;
    }
    
    // Getters
    public String getStatus() {
        return status;
    }
    
    public String getUploadId() {
        return uploadId;
    }
    
    public byte[] getImage() {
        return image;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }

    
}