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
	private static final String MSG_SUCCESS = "Image filtered successfully";
	private static final String MSG_WORKER_CON_SUCC = "Connected to resource: SobelWorker";
	private static final String MSG_CONNECTING_WORKERS = "Connecting with workers";
	private static final String MSG_ERROR_SLICE = "An error occurred while processing slice. ";
	private static final String MSG_ERR_NO_NODES = "Error: no worker-nodes available";
	private static final String MSG_PROCESSED_SLICE = "Slice successfully processed ";
	private static final String MSG_PROCESSING_SLICE = "Processing slice...";
	
	//config parameters	
	private static final String filename = "resources/machine.png";
	private static final String resultFilename ="sobel.png";
	private static final String format = "png";
	private static final String serverIP = "localhost";
	private static final int regPort = 5001;
	private static final int NUMBER_OF_WORKERS = 4;
	
	//internal config variable
	private static final int pixelOverlap = 2;
	private static ArrayList<SobelNode> nodes = new ArrayList<>();
	private static int sliceNumber;
	private static Map<Integer,BufferedImage> slicesToProcess = new HashMap<>();
	private static Map<Integer,BufferedImage> processedSlices = new HashMap<>();
	

	//methods
	public static void main(String[] args) {
		try {
			File file = new File(filename);
			//attributes
			BufferedImage originalImg = ImageIO.read(file);
			
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
			File output = new File(resultFilename);
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
					//the nodeNumber is used to know which slice is given
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
	 * Assigns the slices to each node and monitors their state
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
						//Assumes the error is unrecoverable.
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
				//TODO log that this thread has been interrupted
				Thread.currentThread().interrupt();
			}					
		}
	}

	/**
	 * This function returns slices of the image one by one, slicenumber goes from 0 to sliceCount,
	 * must receive which slice has to return.
	 * Optionally, the slices can overlap
	 * @param sliceNumber which slice is needed
	 * @param sliceCount how many slices in total
	 * @param image image to slice
	 * @param overlap number of pixel of overlap in the slices
	 * @return slice of image
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
     * This function restores an image from slices, assumes that all slices have same width and type,
     * and that they correspond to a image divided in rows.
	 * @param pieces the slices
	 * @param originalHeight height of the image to restore.
	 * @param overlap number of pixel of overlap between slices.
	 * @return reconstructed image.
     *
	 */
	public static BufferedImage restoreImageFromPieces(List<BufferedImage> pieces, int originalHeight, int overlap) {
		int ioverlap = overlap-1;
		int rows = pieces.size();
		//all pieces same widht
		int chunkWidth = pieces.get(0).getWidth();
		int type = pieces.get(0).getType();
		BufferedImage imgResult = new BufferedImage(chunkWidth, originalHeight-2, type);
		
		for (int i = 0; i < rows; i++) {
			int chunkHeight = pieces.get(i).getHeight();
			// if it isn´t the last one
			if(i<rows-1) {
				BufferedImage imgAux = pieces.get(i).getSubimage(0, ioverlap, chunkWidth, chunkHeight-(ioverlap*2));
				imgResult.createGraphics().drawImage(imgAux, 0, (chunkHeight -ioverlap*2) * i , null);
			} else { // if is the last one
				BufferedImage imgAux = pieces.get(i).getSubimage(0, ioverlap, chunkWidth, chunkHeight-(ioverlap*2));
				imgResult.createGraphics().drawImage(imgAux, 0, (originalHeight - (chunkHeight)), null);
			}			
		}
		return imgResult;
	}
	
	private static List<BufferedImage> SlicesMapToList(Map<Integer, BufferedImage> slices) {
		List<BufferedImage> list = new ArrayList<>();
		for (int i = 0; i <slices.size() ; i++) {
			list.add(slices.get(i));
		}
		return list;
	}
}


