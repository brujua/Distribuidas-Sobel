package nodos;

import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class SobelWorker extends UnicastRemoteObject implements RemoteSobel{

	/*
	 * Kernel de convolusión
	 */	
	public static final int[][] Gx = {{-1, 0,1},{-2,0,2},{-1,0,1}};
	public static final int[][] Gy = {{-1, -2, -1},{0,0,0},{1,2,1}};
	public static final int KERNEL_HEIGHT = 3;
	public static final int KERNEL_WIDTH = 3;
	private EstadoNodo estado = EstadoNodo.NO_INICIADO;
	
	
	protected SobelWorker() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}

	//devuelve la imagen procesada
	@Override
	public SerializableImage procesar(SerializableImage image) throws RemoteException, Exception {
		this.estado = EstadoNodo.TRABAJANDO;
		
		BufferedImage img = image.getImg();
		int width = img.getWidth();
		int height = img.getHeight();
		
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
                img.setRGB(i, j, edgeColor);
			}
			
		}
	
		return new SerializableImage(img);
	}


	@Override
	public EstadoNodo getEstado() throws RemoteException {
		return estado;
	}
	
	
	//Funcion que se encarga de convertir un pixel de color a un pixel de escala de gris
	private static int getGrayValue(int rgb) {
		int red= (rgb >> 16) & 0xff;
		int green = (rgb >> 8) & 0xff;
		int blue = rgb & 0xff;
		
		return (int) (0.2126 * red + 0.7152 * green + 0.0722 * blue);
	}
}