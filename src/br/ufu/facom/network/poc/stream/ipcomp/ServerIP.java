package br.ufu.facom.network.poc.stream.ipcomp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.swing.Timer;

import br.ufu.facom.network.poc.stream.RTPPacket;
import br.ufu.facom.network.poc.stream.mjpeg.MjpegFrame;
import br.ufu.facom.network.poc.stream.mjpeg.MjpegInputStream;

public class ServerIP {
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
	private DatagramSocket socket;
	
	

	/**
	 * Construtor
	 */
	public ServerIP(int port, String movie) {

		try {
			socket = new DatagramSocket(port);
		} catch (Exception e) {
			System.err.println("Não foi possível criar  socket");
			System.exit(1);
		}
		
		try {
			videoStream = new MjpegInputStream(new FileInputStream(movie));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		 byte[] bufferRequest = new byte[100];
		 final DatagramPacket packet =  new DatagramPacket(bufferRequest, bufferRequest.length );
		 try {
			while(true){
				socket.receive(packet);
				System.out.println("Receive message...");
				final InetAddress client = packet.getAddress();
				final int client_port = packet.getPort();
				 
				 if(new String(bufferRequest).startsWith(("VIDEO REQUEST"))){
					 new Thread(){
						 public void run() {
							TimerListener tl = new TimerListener();
							Timer timer = new Timer(FRAME_PERIOD, tl);
							timer.setInitialDelay(0);
							timer.setCoalesce(true);
	
							DatagramSocket sockWrite = null;
							
							try {
								sockWrite = new DatagramSocket();
								sockWrite.connect(client,client_port);
							} catch (SocketException e) {
								e.printStackTrace();
							}
							
							tl.timer = timer;
							tl.sockWrite = sockWrite;
							
							timer.start();
							while(timer.isRunning() && sockWrite != null && sockWrite.isConnected()){
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							
							if(timer.isRunning())
								timer.stop();
						 };
					 }.start();
				  }
			 }
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void finalize(){
		if(socket != null){
			socket.close();
		}
	}

	class TimerListener implements ActionListener {
		protected Timer timer;
		protected DatagramSocket sockWrite; 
		

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
	
						DatagramPacket packet = new DatagramPacket(rtp_packet.toBytes(), rtp_packet.toBytes().length);
						// writes the frame via DL-Ontology
						if(sockWrite.isConnected()){
							try{
								sockWrite.send(packet);
							}catch(Exception e2){
								System.err.println("Aborting client...");
								timer.stop();
							}
							System.out.println("Writing frame... "+imageIndex);
						}
	
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
	
	public static void main(String[] args) {
		if(args.length == 2){
			new ServerIP(Integer.parseInt(args[0]),args[1]);
		}
	}
}