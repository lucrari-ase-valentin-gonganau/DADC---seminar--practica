package eu;

import io.javalin.Javalin;
import io.javalin.config.SizeUnit;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import jakarta.servlet.MultipartConfigElement;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

import javax.jms.*;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jgroups.util.UUID;

public class App {
	static final long MAX_FILE_SIZE = 2L * 1024 * 1024 * 1024; // 5 GB
	private static Connection conn;
	static final int PORT = 8081;
	
	static final String BROKER_URL_DEFAULT = "tcp://localhost:61616";
	static final String BROKER_USER_DEFAULT = "admin";
	static final String BROKER_PASSWORD_DEFAULT = "admin";
	


	public static void main(String[] args) {
		try {
			App.configureBroker();
		} catch (JMSException e) {
			System.err.println("Could not connect to JMS broker: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		
		Javalin app = App.configJavalin();
				
		
		app.get("/", ctx -> ctx.result("Javalin REST + JSM Publisher  is running"));
		
		app.post("/upload-and-process",  ctx -> App.upload(ctx));
		
		app.ws("/ws/notifications", ws -> Websocket.notifications(ws));
		
		app.post("/notify/image/status", ctx -> App.updateStatusImage(ctx));

	}
	
	
	protected static void updateStatusImage(Context ctx) {
	
		
		String uploadId = ctx.formParam("uploadId");
		String idInserted = ctx.formParam("idInserted");
		
		
		System.out.println("UploadId received: " + uploadId);
		System.out.println("Id inserted in database received " + idInserted);
		
		if(uploadId == null || idInserted == null) {
			ctx.status(400).result("uploadId and idInserted is required!");
			return ;
		}
		
		
		// convert string to long 
		long rowId = Long.parseLong(idInserted);
		
		Websocket.alertUserThatImageIsReady(uploadId, rowId);
		

		ctx.status(200).result("sent broadcast");

	}
	
	protected static void upload(Context ctx) {
		UploadedFile uploadFile = ctx.uploadedFile("file");
		
		if(uploadFile == null) {
			ctx.status(404).result("The image not found!");
			return;
		}
		
		
		String originalFilename = uploadFile.filename();
		String prefix = "upload-";
		String suffix = "";
		
		String uploadId = UUID.randomUUID().toString();
		
		// extragem extensia
		if (originalFilename != null && originalFilename.contains(".")) {
			suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
		}
		
		
		Path tempFile = null;
		
		try {
			tempFile = Files.createTempFile(prefix, suffix);
		} catch (IOException e) {

			e.printStackTrace();
		}
		
		try (InputStream is = uploadFile.content()) {
			Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {

			e.printStackTrace();
		}
		
		try {
			double zoom = Double.parseDouble(ctx.formParam("zoom"));
			
			
			if (zoom < 0.1 || zoom > 10) {
				throw new Exception("Zoom must be between 0.1 and 10");
			}
			
			App.sendToBroker(tempFile, zoom, uploadId);
			
			ctx.result(uploadId);
		
		} catch (NumberFormatException e) {
			ctx.status(400).result("zoom are mandatory and must be numeric");	
		} catch (Exception e) {
			ctx.status(400).result("zoom must be between 0.1 and 10");
		}
		
	}
	
	
	protected static void sendToBroker(Path tempFile, double zoom, String uploadId) {
		try { 
			Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

			FileInputStream fis = new FileInputStream(tempFile.toString());
			Topic topic = session.createTopic("image.topic");
			// Queue queue = session.createQueue("image.queue");
			MessageProducer producer = session.createProducer(topic);
			
			BytesMessage message = session.createBytesMessage();
			byte[] buffer = new byte[1024 * 1024];
			int read;
			
			while((read = fis.read(buffer)) != -1) {
				message.writeBytes(buffer, 0, read);
			}
			
			message.setDoubleProperty("zoom", zoom);
			
			message.setStringProperty("uploadId", uploadId); // for tracking
			
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			producer.send(message);
			
			System.out.println("Message ID: " + message.getJMSMessageID());
	        System.out.println("Message sent successfully to queue: image.queue");
	        System.out.println("Zoom: (" + zoom + ")");
	        System.out.println("File: " + tempFile);
			
			fis.close();
			session.close();
			producer.close();
	
			
		} catch (JMSException e) {
			System.err.println("Failed to send message to broker!");
			e.printStackTrace();
			
			
			
			
			// 
			
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Failed to read file!");
			e.printStackTrace();
		}

	}
	
	private static void configureBroker() throws JMSException {
        String brokerUrl = System.getenv().getOrDefault("BROKER_URL", App.BROKER_URL_DEFAULT);
        String brokerUser = System.getenv().getOrDefault("BROKER_USER", App.BROKER_USER_DEFAULT);
        String brokerPass = System.getenv().getOrDefault("BROKER_PASSWORD", App.BROKER_PASSWORD_DEFAULT);

        ConnectionFactory factory = (ConnectionFactory) new ActiveMQConnectionFactory(brokerUrl);
        conn = factory.createConnection(brokerUser, brokerPass);
        conn.start();
	}
	
	private static Javalin configJavalin() {
		Javalin app = Javalin.create(config -> {
			
			config.bundledPlugins.enableCors(cors -> {
				cors.addRule(it -> {
					it.anyHost();
					it.allowedOrigins();
				});
			});
			
	
			config.http.maxRequestSize = App.MAX_FILE_SIZE;
			

			config.jetty.multipartConfig.maxFileSize(2, SizeUnit.GB);
			config.jetty.multipartConfig.maxTotalRequestSize(2, SizeUnit.GB);
			config.jetty.multipartConfig.maxInMemoryFileSize(0, SizeUnit.MB); 
			config.jetty.multipartConfig.cacheDirectory("/tmp");
			

			
		}).start(App.PORT);


		app.before(ctx -> {
		    MultipartConfigElement multipartConfigElement =
		    		new MultipartConfigElement(
	                    "/tmp",
	                    Long.MAX_VALUE,   // max file size
	                    Long.MAX_VALUE,   // max request size
	                    0
		            );
		    ctx.req().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
		    
			System.out.println("Applying multipart config...");
			System.out.println("Attribute: " + ctx.req().getAttribute("org.eclipse.jetty.multipartConfig"));
			
		});

		
		return app;
	}

}


