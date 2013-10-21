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

import sun.misc.Signal;
import sun.misc.SignalHandler;
import br.ufu.facom.network.dlontology.EntitySocket;
import br.ufu.facom.network.dlontology.WorkspaceSocket;
import br.ufu.facom.network.dlontology.exception.DTSException;

public class Client {

	BufferedReader bufferedReader;
	BufferedWriter bufferedWriter;

	static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
	
	private static EntitySocket entity;
	private static WorkspaceSocket workspace;
	private WorkspaceSocket secondSocket;
	private String titleStream;
	
	private Thread streamThread;
	
	/**
	 * GUI
	 */
	 JFrame f = new JFrame("Client");
	 JPanel mainPanel = new JPanel();
	 JLabel iconLabel = new JLabel();
	 ImageIcon icon;
	 
	public Client(String interf, String titleClient, String titleStream, String secondInterf) throws Exception {
		this.titleStream = titleStream;

		entity = new EntitySocket(interf, titleClient);
		workspace = new WorkspaceSocket(interf, titleStream);
		if(!entity.open() || !workspace.open()) {
			throw new Exception("Finsocket open fail");
		} 
		
		if(secondInterf != null) {
			secondSocket = new WorkspaceSocket(secondInterf, titleStream);
			if(secondSocket.open()) {
				throw new Exception("Finsocket open fail");
			}
		}
			
		try{
			entity.register();
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Entidade provavelmenta já registrada.");
		}
		
		try {
			
			workspace.attach(entity);
			System.out.println("Attached to workspace " + workspace.getTitle());
			
		} catch (DTSException e) {
			
			System.out.println("Failed to attach, trying to create");
			
			workspace.createOnDts(entity);
			System.out.println("Attached to workspace " + workspace.getTitle());
			
		}
		
		createView();
	
		startThread(workspace);
		
		if(secondInterf != null) {
			startThread(secondSocket);
		}
	}

	private void startThread(final WorkspaceSocket workspaceSocket) {
		streamThread = new Thread(){
	    	@Override
	    	public void run() {
	    	while(!streamThread.isInterrupted()){
			try {
				// receive the DP from the socket:
				byte buf[] = workspaceSocket.recieve();
				int size = buf.length;
				
				// create an RTPpacket object from the DP
				RTPPacket rtp_packet = new RTPPacket(buf, size);
	
				// get the payload bitstream from the RTPpacket object
				int payload_length = rtp_packet.getPayloadLength();
				byte[] payload = new byte[payload_length];
				rtp_packet.getPayload(payload);
	
					
				// get an Image object from the payload bitstream
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Image image = toolkit.createImage(payload, 0, payload_length);
	
				// display the image as an ImageIcon object
				icon = new ImageIcon(image);
				
				if(icon.getIconWidth() > 0){//Estava chegando imagens com tamanho menor que 0
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
	        	try {
					finalize();
				} catch (Throwable e1) {
					e1.printStackTrace();
				}
	            System.exit(0);
	     }});
	    
	}
	
	public static void endEntity(){
		try {
			workspace.detach();
			System.out.println("Detached.");
		} catch (Exception e) {
			System.out.println("Failed to detach entity.");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
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

		if(args.length == 4){
			new Client(args[0], args[1], args[2], args[3]);
		} else if (args.length == 3) {
			new Client(args[0], args[1], args[2], null);			
		} else {
			System.err.println("Usage:\n<interface> <titleClient> <titleStream> [second interface]");
		}
	}
}