package client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import common.RemoteSobel;

public class SobelCoordinator {

	//msg constants
	private static final String MSG_SUCCESS = "Todo OK";
	private static final String MSG_WORKER_CON_SUCC = "Conectado al recurso SobelWorker";
	private static final String MSG_CONNECTING_WORKERS = "Conectando con los worker";
	private static final String MSG_ERROR_SLICE = "Error procesando la slice ";
	private static final String MSG_ERR_NO_NODES = "Error: No hay nodos para trabajar";
	private static final String MSG_PROCESSED_SLICE = "Procesada exitosamente slice ";
	private static final String MSG_PROCESSING_SLICE = "Procesando slice ";
	
	//config parameters	
	public static final String filename = "resources/test.jpg";
	public static final String format = "jpg";
	public static final String serverIP = "192.168.0.40";
	public static final int regPort = 5020;
	public static final int pixelOverlap = 2;
	private static final int NUMBER_OF_WORKERS = 4;
	
	//attributes
	public static BufferedImage originalImg;
	public static ArrayList<SobelNode> nodes = new ArrayList<SobelNode>();
	public static int sliceNumber;
	public static Map<Integer,BufferedImage> slicesToProcess = new HashMap<>();
	public static Map<Integer,BufferedImage> processedSlices = new HashMap<>();
	

	//methods
	public static void main(String[] args) {
		try {
			File file = new File(filename);
			originalImg = ImageIO.read(file);
			
			//divide the image in as many slices as nodes are.
			//And start the asynchronous process
			initializeNodes();
			sliceNumber = nodes.size();
			//slice the image and put the slices in the map waiting to process
			for (int i = 0; i < sliceNumber; i++) {
				slicesToProcess.put(i, getSlice(i, sliceNumber, originalImg,pixelOverlap));	
			}
			//wait for the results and put them in processedSlices
			processSlices();
			BufferedImage imgFinal = restoreImageFromPieces(SlicesMapToList(processedSlices), originalImg.getHeight(), pixelOverlap);
			File output = new File("sobel.png");
			ImageIO.write(imgFinal, "png", output);
			System.out.println(MSG_SUCCESS);
		}
		catch(IllegalStateException e) {
			System.out.println(e.getMessage());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void initializeNodes(){
		try {
			Registry registry = LocateRegistry.getRegistry(serverIP,regPort);
			System.out.println(MSG_CONNECTING_WORKERS);
			//for each remote available, create a node and add it to the list
			for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
				RemoteSobel worker = (RemoteSobel) registry.lookup("sobel"+i);
				if(worker != null) {
					//the nodeNumber its used to know witch slice its given to it
					//the first time they are given in ascending sequence
					nodes.add(new SobelNode(worker, i));
					System.out.println(MSG_WORKER_CON_SUCC+i);
				}
			}			
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Assign the slices to each node and monitors their state
	 */
	private static void processSlices(){
		while(true) {
			if(nodes.size()==0) {
				throw new IllegalStateException(MSG_ERR_NO_NODES);
			}
			//iterate over the nodes
			Iterator<SobelNode> nodesIt = nodes.iterator();
			while(nodesIt.hasNext()) {
				SobelNode node = nodesIt.next();
				switch(node.getState()) {
					case AVAILABLE:{
						if(!slicesToProcess.isEmpty()) {
							Iterator<Integer> iterator = slicesToProcess.keySet().iterator();
							//retrieve slice and its number
							Integer sliceN = iterator.next();
							//Set node number to the number of slice
							node.setNodeNumber(sliceN);
							//start processing
							node.startSobel(slicesToProcess.remove(sliceN),format);
							System.out.println(MSG_PROCESSING_SLICE + sliceN);
						}
						break;
					}
					case FINISHED:{
						processedSlices.put(node.getNodeNumber(), node.getProcessedImg());
						node.done();
						System.out.println(MSG_PROCESSED_SLICE + node.getNodeNumber());
						break;
					}
					case WORKING:
						break;
					case ERROR: {
						//TODO log the error
						//the slice will be pending
						slicesToProcess.put(node.getNodeNumber(), node.getImg());
						//the node its removed from the working nodes
						//TODO depending on the error might be the correct action or not.
						nodesIt.remove();
						System.out.println(MSG_ERROR_SLICE + node.getNodeNumber());
						break;
					}
				}
			}//end foreach node
			
			//if all slices were processed, return
			if(processedSlices.size() == sliceNumber) {
				return;
			}
			// else wait for processing
			try {
				Thread.sleep(250);
			}catch(InterruptedException e) {
				//log that this thread has been interrupted
				Thread.currentThread().interrupt();
			}					
		}
	}

	/**
	 * funcion que devuelve los pedazos de imagenes de a uno, slicenumber va de 0 a sliceCount
	 * y permite indicarle que pedazo se quiere recuperar.
	 * Devuelve las imagenes solapadas opcionalmente
	 * @param sliceNumber numero de pedazo
	 * @param sliceCount cantidad de pedazos
	 * @param image
	 * @param overlap number of pixel of overlap
	 * @return pedazo de la imagen, retorna null si la altura de las imagenes va a ser menor a KERNEL_HEIGHT
	 *  
	 */
	public static BufferedImage getSlice (int sliceNumber, int sliceCount, BufferedImage image,int overlap){
		if(sliceNumber >= sliceCount || sliceNumber<0) {
			throw new IllegalArgumentException("sliceNumber no es un indice válido");
		}
		int originalHeight = image.getHeight();
	    int xInicio   = 0;
	    int xsize  = image.getWidth();
	    int ysize = 0;
	    //posiciones inicio y fin para que queden solapas por dos pixeles
	    int yposInicio   = sliceNumber * (int) (originalHeight / sliceCount);
	    int yposFin = ((sliceNumber + 1) * (int) (originalHeight / sliceCount)) +overlap;
	    
	    //Si estoy en el ultimo pedazo
	    if(sliceNumber == sliceCount-1) {
	    	ysize = originalHeight - yposInicio;
	    } else { //sino
	    	ysize = yposFin - yposInicio; 
	    }   
	    return image.getSubimage(xInicio, yposInicio, xsize, ysize);
	}
	
	/**
	 * @param pieces
	 * @param originalHeight  :la altura original de la imagen a recuperar
	 * @param overlap :la cantidad de pixeles de overlap  
	 * @return
	 * Función que retorna la union de los pedazo de imagenes, se asume que todos los pedazos
	 * tienen el mismo widht y tipo. Que corresponden a una imagen
	 * divida solo de a filas
	 */
	public static BufferedImage restoreImageFromPieces(List<BufferedImage> pieces, int originalHeight, int overlap) {
		int ioverlap = overlap-1;
		int rows = pieces.size();
		//asumo que todos tienen igual width,  tipo
		int chunkWidth = pieces.get(0).getWidth();
		int type = pieces.get(0).getType();
		//inicializo la imagen resultante
		BufferedImage imgResult = new BufferedImage(chunkWidth, originalHeight-2, type);
		
		for (int i = 0; i < rows; i++) {
			int chunkHeight = pieces.get(i).getHeight();
			//si no es la ultima
			if(i<rows-1) {
				BufferedImage imgAux = pieces.get(i).getSubimage(0, ioverlap, chunkWidth, chunkHeight-(ioverlap*2));
				imgResult.createGraphics().drawImage(imgAux, 0, (chunkHeight -ioverlap*2) * i , null);
			} else { //si es la ultimo
				BufferedImage imgAux = pieces.get(i).getSubimage(0, ioverlap, chunkWidth, chunkHeight-(ioverlap*2));
				imgResult.createGraphics().drawImage(imgAux, 0, (originalHeight - (chunkHeight)), null);
			}			
		}
		return imgResult;
	}
	
	private static List<BufferedImage> SlicesMapToList(Map<Integer, BufferedImage> slices) {
		List<BufferedImage> list = new ArrayList<BufferedImage>();
		for (int i = 0; i <slices.size() ; i++) {
			list.add(slices.get(i));
		}
		return list;
	}
}


