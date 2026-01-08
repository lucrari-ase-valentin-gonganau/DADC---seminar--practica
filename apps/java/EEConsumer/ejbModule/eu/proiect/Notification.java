package eu.proiect;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Notification {
	
	
	public static void notifyApp(String uploadId, long idInserted) {
		String url = System.getenv().getOrDefault("URL_STATUS_IMAGE_PROCESSING", "http://localhost");
		int port =  Integer.parseInt(System.getenv().getOrDefault("PORT_STATUS_IMAGE_PROCESSING", "8081"));
		
		
		HttpClient client = HttpClient.newHttpClient();
		String formData = "uploadId=" + uploadId + "&idInserted="+idInserted;
		
		URI pathUrl = URI.create(url + ":" + port + "/notify/image/status");
		HttpRequest request = (HttpRequest) HttpRequest.newBuilder()
				.uri(pathUrl)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(formData))
				.build();
				
		
		System.out.println("Path: " + pathUrl +": formData: " + formData);
		
		try {
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			
			if(response.statusCode() == 200) {
				System.out.println("Notification sent for uploadId: " + uploadId);
			} else {
				System.err.println("Failed to notify: " + response.statusCode());
			}
			
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
