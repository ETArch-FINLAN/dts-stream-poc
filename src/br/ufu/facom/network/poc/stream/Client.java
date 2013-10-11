package br.ufu.facom.network.poc.stream;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import br.ufu.facom.network.dlontology.EntitySocket;
import br.ufu.facom.network.dlontology.WorkspaceSocket;
import br.ufu.facom.network.dlontology.exception.DTSException;

public class Client {
	//Timer timer; // timer used to receive data
	byte[] buf; // buffer used to store data received

	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;

	static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
	
	private EntitySocket entity;
	private WorkspaceSocket workspace;
	private String titleStream;
	
	private Thread streamThread;
	/**
	 * GUI
	 */
	 JFrame f = new JFrame("Client");
	 JPanel mainPanel = new JPanel();
	 JLabel iconLabel = new JLabel();
	 ImageIcon icon;
	
	public Client(String interf, String titleClient, String titleStream, boolean viewEnabled) throws Exception {
		this.titleStream = titleStream;

		// allocate enough memory for the buffer used to receive data from the server
		buf = new byte[15000];
		
		
		entity = new EntitySocket(interf, titleClient);
		if(!entity.open()) {
			throw new Exception("Finsocket open fail");
		} 
		
		entity.register();
		
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
		
		if(viewEnabled) {
			createView();
		}
	
		startThread(viewEnabled);
	}

	private void startThread(final boolean viewEnabled) {
		streamThread = new Thread(){
	    	@Override
	    	public void run() {
	    	while(!streamThread.isInterrupted()){
			try {
				// receive the DP from the socket:
				byte buf[] = workspace.recieve();
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

	private void endSocket() {//TODO analisar código
		/*if(finSocket != null){
			//finSocket.disjoinWorkspace(titleStream);
			//finSocket.unregister();
			finSocket.close();
		}*/
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length == 4){
			new Client(args[0], args[1] , args[2], Boolean.getBoolean(args[3].toLowerCase()));
		}else
			System.err.println("Wrong usage");
	}
}