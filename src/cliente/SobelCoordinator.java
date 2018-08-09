package cliente;

import java.awt.image.BufferedImage;
import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import common.RemoteSobel;
import common.SerializableImage;

public class SobelCoordinator {

	private static final String MSG_SUCCESS = "Todo OK";
	private static final String MSG_WORKER_CON_SUCC = "Conectado al recurso SobelWorker";
	private static final String MSG_CONNECTING_WORKERS = "Conectando con los worker";
	
	//nasty constants that need refactor
	private static final int NUMBER_OF_WORKERS = 4;
	private static final int KERNEL_HEIGHT = 3;
	
	public static final String filename = "resources/machine.PNG";
	public static BufferedImage originalImg;
	public static  ArrayList<BufferedImage> imgSlices = new ArrayList<BufferedImage>();
	public static int sliceNumber;
	public static Map<Integer,BufferedImage> processedSlices = new HashMap<Integer,BufferedImage>();
	public static ArrayList<SobelNode> nodes = new ArrayList<SobelNode>();
	
	public static void main(String[] args) {
		/*try {
			conectarWorkers();
			File file = new File(filename);
			originalImg = ImageIO.read(file);
			for (int i = 0; i < workers.size(); i++) {
				RemoteSobel worker = workers.get(i);
				BufferedImage pedazo = getSlice(i, workers.size(), originalImg,2);
				SerializableImage pedazoProcesado=worker.procesar(new SerializableImage(pedazo));	
				pedazosImg.add(pedazoProcesado.getImg());
			}
			BufferedImage imgFinal = restoreImageFromPieces(pedazosImg, originalImg.getHeight(), 1);
			File output = new File("sobelConcatVerdad3.png");
			ImageIO.write(imgFinal, "png", output);
			System.out.println("Todo OK");
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		try {
			File file = new File(filename);
			originalImg = ImageIO.read(file);
			
			//divide the image in as many slices as nodes are.
			//And start the asynchronous process
			initializeNodes();
			sliceNumber = nodes.size();
			for (int i = 0; i < nodes.size(); i++) {
				SobelNode node = nodes.get(i);
				BufferedImage imgPiece = getSlice(i, sliceNumber, originalImg,2);
				node.startSobel(imgPiece);	
			}
			//wait for the results and put them in processedSlices
			gatherResults();
			BufferedImage imgFinal = restoreImageFromPieces(mapToList(processedSlices), originalImg.getHeight(), 1);
			File output = new File("sobelConcatVerdad3.png");
			ImageIO.write(imgFinal, "png", output);
			System.out.println(MSG_SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	

	private static List<BufferedImage> mapToList(Map<Integer, BufferedImage> slices) {
		List<BufferedImage> list = new ArrayList<BufferedImage>();
		for (int i = 0; i <slices.size() ; i++) {
			list.add(slices.get(i));
		}
		return list;
	}



	public static void initializeNodes(){
		try {
			Registry registry = LocateRegistry.getRegistry("localhost",5002);
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
	
	private static void gatherResults() {
		while(true) {
			for (SobelNode node : nodes) {
				switch(node.getState()) {
					case FINALIZADO:{
						processedSlices.put(node.getNodeNumber(), node.getProcessedImg());
						node.done();
						break;
					}
					case TRABAJANDO:
						break;
					case DESCONECTADO:
					case ERROR: {
						findNodeToProccesImg(node.getImg());
						nodes.remove(node);
						break;
					}
					default:
						break;
				}
				//if all slices were processed, return
				if(processedSlices.size() == sliceNumber) {
					return;
				}
				//sleep 100 miliseconds
				try {
					Thread.sleep(100);
				}catch(InterruptedException e) {
					//log that this thread has been interrupted
					
					Thread.currentThread().interrupt();
				}	
			}	
		}
	}
	
	private static void findNodeToProccesImg(BufferedImage img) {
		while(true) {
			for(SobelNode node:nodes) {
				
			}
		}
		
	}



	/**
	 * funcion que devuelve los pedazos de imagenes de a uno, slicenumber va de 0 a sliceCount
	 * y permite indicarle que pedazo se quiere recuperar.
	 * Devuelve las imagenes solapadas por 2 pixeles
	 * @param sliceNumber numero de pedazo
	 * @param sliceCount cantidad de pedazos
	 * @param image
	 * @return pedazo de la imagen, retorna null si la altura de las imagenes va a ser menor a KERNEL_HEIGHT
	 *  
	 */
	public static BufferedImage getSlice (int sliceNumber, int sliceCount, BufferedImage image,int overlap){
		if(((int)image.getHeight()/sliceCount)<KERNEL_HEIGHT) {
			return null;
		}
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
		int cols =1;
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
				BufferedImage imgAux = pieces.get(i).getSubimage(0, overlap, chunkWidth, chunkHeight-(overlap*2));
				imgResult.createGraphics().drawImage(imgAux, 0, (chunkHeight -overlap*2) * i , null);
			} else { //si es la ultimo
				BufferedImage imgAux = pieces.get(i).getSubimage(0, overlap, chunkWidth, chunkHeight-(overlap*2));
				imgResult.createGraphics().drawImage(imgAux, 0, (originalHeight - (chunkHeight)), null);
			}
			
			
		}
		return imgResult;
	}
}
