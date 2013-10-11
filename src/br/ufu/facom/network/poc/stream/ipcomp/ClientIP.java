package br.ufu.facom.network.poc.stream.ipcomp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import br.ufu.facom.network.poc.stream.RTPPacket;

public class ClientIP {
	//Timer timer; // timer used to receive data
	byte[] buf; // buffer used to store data received

	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;

	static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
	
	private DatagramSocket socket;
	
	private Thread streamThread;
	/**
	 * GUI
	 */
	 JFrame f = new JFrame("Client");
	 JPanel mainPanel = new JPanel();
	 JLabel iconLabel = new JLabel();
	 ImageIcon icon;
	
	public ClientIP(String hostIp, int port, boolean viewEnabled) {

		// allocate enough memory for the buffer used to receive data from the server
		buf = new byte[15000];
		
		try {
			socket = new DatagramSocket();
			InetAddress address = InetAddress.getByName(hostIp);
			
			byte[] sendData = new byte[1024];
		      String sentence = "VIDEO REQUEST";
		      sendData = sentence.getBytes();
		      System.out.println("Sending...");
		      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
		      socket.send(sendPacket);
		      
		      if(viewEnabled)
					createView();
			
				startThread(viewEnabled);
			
		} catch (Exception e) {
			System.err.println("Não foi possível criar  socket");
			System.exit(1);
		}
	}

	private void startThread(final boolean viewEnabled) {
		streamThread = new Thread(){
	    	@Override
	    	public void run() {
	    		int cont=1;
	    	while(!streamThread.isInterrupted()){
			try {
				byte[] receiveData = new byte[7000];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(receivePacket);
				System.out.println("Receiving frame "+(cont++)+"...");
				
				byte buf[] = receivePacket.getData();
				int size = buf.length;
	
				// create an RTPpacket object from the DP
				RTPPacket rtp_packet = new RTPPacket(buf, size);
				
				// get the payload bitstream from the RTPpacket object
				int payload_length = rtp_packet.getPayloadLength();
				byte[] payload = new byte[payload_length];
				rtp_packet.getPayload(payload);
	
				if(viewEnabled){
					// get an Image object from the payload bitstream
					Toolkit toolkit = Toolkit.getDefaultToolkit();
					Image image = toolkit.createImage(payload, 0, payload_length);
		
					// display the image as an ImageIcon object
					icon = new ImageIcon(image);
					iconLabel.setIcon(icon);
				}
			}catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("Exception caught: " + ex);
			}
	    	}
	    	}
	    };
	    
	    streamThread.start();
	}

	private void createView() {
		//Image display label
	    iconLabel.setIcon(null);
	    
	    //frame layout
	    mainPanel.setLayout(null);
	    mainPanel.add(iconLabel);
	    iconLabel.setBounds(0,0,380,280);

	    f.getContentPane().add(mainPanel, BorderLayout.CENTER);
	    f.setSize(new Dimension(390,320));
	    f.setVisible(true);
	    f.addWindowListener(new WindowAdapter() {
	        public void windowClosing(WindowEvent e) {
	        	endSocket();
	            System.exit(0);
	     }});
	    
	}	

	private void endSocket() {
		if(streamThread != null)
			streamThread.interrupt();
		
		if(socket != null){
			socket.close();
		}
	}
	
	public static void main(String[] args) {
		if(args.length == 2){
			new ClientIP(args[0],Integer.parseInt(args[1]),true);
		}else if(args.length == 3){
			new ClientIP(args[0],Integer.parseInt(args[1]), Boolean.getBoolean(args[2].toLowerCase()));
		}
	}
}