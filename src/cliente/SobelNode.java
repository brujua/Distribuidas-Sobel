package cliente;

import java.awt.image.BufferedImage;
import java.rmi.RemoteException;

import common.EstadoNodo;
import common.RemoteSobel;
import common.SerializableImage;
/*
 * Class responsible for monitoring the worker and the state of the process
*/
public class SobelNode {
	private EstadoNodo state;
	private RemoteSobel rWorker;
	private BufferedImage processedImg;
	private Thread thread;
	private BufferedImage img;
	private int nodeNumber; //sort of id
	
	

	public SobelNode(RemoteSobel worker, int nodeNumber) {
		this.rWorker = worker;
		this.state = EstadoNodo.DISPONIBLE;
		this.processedImg = null;
		this.thread=null;
		this.img = null;
		this.nodeNumber = nodeNumber;
		
	}

	public void startSobel(BufferedImage img) {
		if(this.state== EstadoNodo.DISPONIBLE) {			
			this.state = EstadoNodo.TRABAJANDO;
			this.thread = new Thread() {
				@Override
				public void run() {
					try {
						processedImg = rWorker.procesar(new SerializableImage(img)).getImg();
						state = EstadoNodo.FINALIZADO;
					} catch (Exception e) {
						state = EstadoNodo.ERROR;
						e.printStackTrace();
					}
				}
			};
			this.thread.start();
		}		
	}
	
	public EstadoNodo getState() {
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
		if(this.state != EstadoNodo.TRABAJANDO) {
			this.state = EstadoNodo.DISPONIBLE;
		}
	}
}
