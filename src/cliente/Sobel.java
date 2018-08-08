package cliente;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class Sobel {
	
	// Definición del kernel de sobel
	public static final int[][] Gx = {{-1, 0,1},{-2,0,2},{-1,0,1}};
	public static final int[][] Gy = {{-1, -2, -1},{0,0,0},{1,2,1}};
	public static final int KERNEL_HEIGHT = 3;
	public static final int KERNEL_WIDTH = 3;
	public static final String filename = "resources/machine.PNG";
	public static  ArrayList<BufferedImage> pedazosImg = new ArrayList<BufferedImage>();
	public static BufferedImage originalImg;
	
	
	
	public static void main (String[] args) {
		
		try {			
			File file = new File(filename);
			originalImg = ImageIO.read(file);
	
			for(int i=0;i<8;i++) {
				File output = new File("sobel"+i+".png");
				File output2 = new File("pedazo"+i+".png");
				BufferedImage pedazo = getSlice(i, 8, originalImg,2);
				ImageIO.write(pedazo, "png", output2);
				BufferedImage img =procesar(pedazo);
				ImageIO.write(img, "png", output);
				pedazosImg.add(img);
			}
			File output = new File("sobelConcatVerdad2.png");
			ImageIO.write(restoreImageFromPieces(pedazosImg,originalImg.getHeight(),1), "png", output);
			System.out.println("Todo OK");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
 
	/**
	 * 
	 * @param sliceNumber numero de pedazo
	 * @param sliceCount cantidad de pedazos
	 * @param image
	 * @return pedazo de la imagen, retorna null si la altura de las imagenes va a ser menor a KERNEL_HEIGHT
	 * funcion que devuelve los pedazos de imagenes de a uno, slicenumber va de 0 a sliceCount
	 * y permite indicarle que pedazo se quiere recuperar.
	 * Devuelve las imagenes solapadas por 2 pixeles
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
	public static BufferedImage restoreImageFromPieces(ArrayList<BufferedImage> pieces, int originalHeight, int overlap) {
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
	
	//devuelve la imagen procesada
	/**
	 * @param img :
	 * @return
	 */
	public static BufferedImage procesar(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight(); 
		BufferedImage imgSobel = new BufferedImage(width, height, img.getType());
		
		//guardo los valores obtenidos de aplicar el filtro en este arreglo
		int[][] edgeColors = new int[width][height];
        int maxGradient = -1; //variable utilizada para realizar la normalizacion
        
		for(int i=1;i<width-1;i++) {
			for(int j=1;j<height-1;j++) {
				//recupero la matriz del kernel y la paso a escala de grises
				int val00 = getGrayValue(img.getRGB(i - 1, j - 1));
	            int val01 = getGrayValue(img.getRGB(i - 1, j));
	            int val02 = getGrayValue(img.getRGB(i - 1, j + 1));
	            int val10 = getGrayValue(img.getRGB(i, j - 1));
	            int val11 = getGrayValue(img.getRGB(i, j));
	            int val12 = getGrayValue(img.getRGB(i, j + 1));
	            int val20 = getGrayValue(img.getRGB(i + 1, j - 1));
	            int val21 = getGrayValue(img.getRGB(i + 1, j));
	            int val22 = getGrayValue(img.getRGB(i + 1, j + 1));
	            
	            //Aplico el filtro
	            int gradientX = ((Gx[0][0] * val00) + (Gx[0][1] * val01) + (Gx[0][2] * val02)
	            		+ (Gx[1][0] * val10) + (Gx[1][1] * val11) + (Gx[1][2] * val12)
	            		+ (Gx[2][0] * val20) + (Gx[2][1] * val21) + (Gx[2][2] * val22));
	            int gradientY = ((Gy[0][0] * val00) + (Gy[0][1] * val01) + (Gy[0][2] * val02)
	            		+ (Gy[1][0] * val10) + (Gy[1][1] * val11) + (Gy[1][2] * val12)
	            		+ (Gy[2][0] * val20) + (Gy[2][1] * val21) + (Gy[2][2] * val22));
	          
	            int valResult = (int) Math.sqrt((gradientX * gradientX)+(gradientY * gradientY));
	            //Actualizo valor de normalización
	            if(maxGradient < valResult) {
	            	maxGradient = valResult;
	            }
	            
	            edgeColors[i][j]=valResult;
			}			
		}
		//calculo el valor de escala para que caiga entre 0 y 255
		double scale = 255.0 / maxGradient;
		//llevo los resultados obtenidos a la escala de grises
		for (int i = 1; i < width-1; i++) {
			for (int j = 1; j < height-1; j++) {
				int edgeColor = edgeColors[i][j];
				//lo normalizo a un valor entre 0 y 255
                edgeColor = (int)(edgeColor * scale);
                //rgb 
                edgeColor = 0xff000000 | (edgeColor << 16) | (edgeColor << 8) | edgeColor;
                //seteo el nuevo valor
                imgSobel.setRGB(i, j, edgeColor);
			}
			
		}
		System.out.println("Max gradrient: "+maxGradient);
		return imgSobel; 
		
	}
	
	
	//Funcion que se encarga de convertir un pixel de color a un pixel de escala de gris
	public static int getGrayValue(int rgb) {
		int red= (rgb >> 16) & 0xff;
		int green = (rgb >> 8) & 0xff;
		int blue = rgb & 0xff;
		
		return (int) (0.2126 * red + 0.7152 * green + 0.0722 * blue);
	}
	
}
