package br.ufu.facom.network.poc.stream;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.Timer;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import br.ufu.facom.network.dlontology.EntitySocket;
import br.ufu.facom.network.dlontology.WorkspaceSocket;
import br.ufu.facom.network.dlontology.exception.DTSException;
import br.ufu.facom.network.poc.stream.mjpeg.MjpegFrame;
import br.ufu.facom.network.poc.stream.mjpeg.MjpegInputStream;

public class Server {
	/**
	 * Constants
	 */
	private static final int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
	private static final int FRAME_PERIOD = 70; // Frame period of the video to stream, in ms
	

	/**
	 * Instance variables
	 */
	private int imageIndex = 0; // image index of the image currently transmitted
	private MjpegInputStream videoStream; // VideoStream object used to access video frames

	private Timer timer; // timer used to send the images at the video frame rate

	private static EntitySocket entity;
	private WorkspaceSocket workspace;
	private String titleStream;
	
	/**
	 * Construtor
	 * @throws Exception 
	 */
	public Server(String interf, String titleServer, String titleStream, String movie) throws Exception {
		this.titleStream = titleStream;
		
		// init Timer
		timer = new Timer(FRAME_PERIOD, new TimerListener());
		timer.setInitialDelay(0);
		timer.setCoalesce(true);

		entity = new EntitySocket(interf, titleServer);
		if(!entity.open()) {
			throw new Exception("Finsocket open fail");
		} 
		
		try{
			entity.register();
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Entidade provavelmenta já registrada.");
		}
		
		workspace = new WorkspaceSocket(interf, titleStream);
		if(!workspace.open()) {
			throw new Exception("Finsocket open fail");
		}

		try {
			
			workspace.attach(entity);
			System.out.println("Attached to workspace " + workspace.getTitle());
			
		} catch (DTSException e) {
			
			System.out.println("Failed to attach, trying to create");
			
			workspace.createOnDts(entity);
			System.out.println("Attached to workspace " + workspace.getTitle());
			
		}
		
		try {
			videoStream = new MjpegInputStream(new FileInputStream(movie));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		timer.start();
		
		while(timer.isRunning()){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void endEntity(){
		try {
			entity.unregister();
			System.out.println("Entity unregistered.");
		} catch (Exception e) {
			System.out.println("Failed to unregister entity.");
			e.printStackTrace();
		}
	}

	class TimerListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {

			MjpegFrame frame;
			try {
				frame = videoStream.readMjpegFrame();
			
				if (frame != null) {
	
					// update current imagenb
					imageIndex++;
	
					try {
						// get next frame to send from the video, as well as its size
						int image_length = frame.getLength();
	
						// builds an RTPpacket object containing the frame
						RTPPacket rtp_packet = new RTPPacket(MJPEG_TYPE, imageIndex, imageIndex * FRAME_PERIOD, frame.getBytes(), image_length);
						
						// writes the frame via DL-Ontology
						workspace.send(rtp_packet.toBytes());
	
					} catch (Exception ex) {
						System.err.println("Exception caught: " + e);
						ex.printStackTrace();
						System.exit(0);
					}
				} else {
					// if we have reached the end of the video file, stop the timer
					timer.stop();
				}
			} catch (IOException e1) {
				System.err.println("Cannot read the Frame...");
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length == 4){
			
			//Capturando sinal de saida
			Signal.handle(new Signal("INT"), new SignalHandler () {
				public void handle(Signal sig) {
					try {
						endEntity();
						System.exit(0);
					} catch (Throwable e) {
						System.out.println("erro");
						e.printStackTrace();
					}
				}
			});
			
			new Server(args[0], args[1], args[2], args[3]);
		} else {
			System.err.println("Usage:\n<interface> <titleServer> <titleStream> <movie>");
		}
	}
}