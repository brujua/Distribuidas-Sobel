package client;

import java.awt.image.BufferedImage;
import common.NodeState;
import common.RemoteSobel;
import common.SerializableImage;
/*
 * Class responsible for monitoring the worker and the state of the process
 * once it is finished the state change to finished and the processed img can be retrieved
 * with getProcessedImg()
 * With the method done() the node goes back to available state
*/
public class SobelNode {
	private NodeState state;
	private RemoteSobel rWorker;
	private BufferedImage processedImg;
	private Thread thread;
	private BufferedImage img;
	private int nodeNumber; //sort of id
	
	

	public SobelNode(RemoteSobel worker, int nodeNumber) {
		this.rWorker = worker;
		this.state = NodeState.AVAILABLE;
		this.processedImg = null;
		this.thread=null;
		this.img = null;
		this.nodeNumber = nodeNumber;
		
	}

	public void startSobel(BufferedImage img, String format) {
		if(this.state==NodeState.WORKING)
			throw new IllegalStateException();
		this.state = NodeState.WORKING;
		this.thread = new Thread() {
			@Override
			public void run() {
				try {
					//blocking rmi call
					processedImg = rWorker.procesar(new SerializableImage(img, format)).getImg();
					state = NodeState.FINISHED;
				} catch (Exception e) {
					state = NodeState.ERROR;
					e.printStackTrace(); //TODO log
				}
			}
		};
		this.thread.start();		
	}
	
	public NodeState getState() {
		return this.state;
	}
	
	public BufferedImage getProcessedImg() {
		return this.processedImg;
	}
	
	public int getNodeNumber() {
		return nodeNumber;
	}

	public void setNodeNumber(int nodeNumber) {
		this.nodeNumber = nodeNumber;
	}

	public BufferedImage getImg() {
		return img;
	}

	public void done() {
		if(this.state != NodeState.WORKING) {
			this.state = NodeState.AVAILABLE;
		}
	}
}
