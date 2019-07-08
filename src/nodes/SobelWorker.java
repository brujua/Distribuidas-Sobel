package nodes;

import java.awt.image.BufferedImage;
import java.rmi.RemoteException;

import common.RemoteSobel;
import common.SerializableImage;

public class SobelWorker implements RemoteSobel{

	// Convolution Kernel
	private static final int[][] Gx = {{-1, 0,1},{-2,0,2},{-1,0,1}};
	private static final int[][] Gy = {{-1, -2, -1},{0,0,0},{1,2,1}};
	private static final int KERNEL_HEIGHT = 3;
	private static final int KERNEL_WIDTH = 3;
	
	
	protected SobelWorker(){
		super();
	}

	@Override
	public SerializableImage process(SerializableImage image){
		
		BufferedImage img = image.getImg();
		int width = img.getWidth();
		int height = img.getHeight();
		
		if(height < KERNEL_HEIGHT || width < KERNEL_WIDTH) {
			throw new IllegalArgumentException();
		}
		
		// save values obtained from applying the filter in this array
		int[][] edgeColors = new int[width][height];
        int maxGradient = -1; // utilized in Normalization
        
		for(int i=1;i<width-1;i++) {
			for(int j=1;j<height-1;j++) {
				// retrieve the kernel matrix and move to gray scale
				int val00 = getGrayValue(img.getRGB(i - 1, j - 1));
	            int val01 = getGrayValue(img.getRGB(i - 1, j));
	            int val02 = getGrayValue(img.getRGB(i - 1, j + 1));
	            int val10 = getGrayValue(img.getRGB(i, j - 1));
	            int val11 = getGrayValue(img.getRGB(i, j));
	            int val12 = getGrayValue(img.getRGB(i, j + 1));
	            int val20 = getGrayValue(img.getRGB(i + 1, j - 1));
	            int val21 = getGrayValue(img.getRGB(i + 1, j));
	            int val22 = getGrayValue(img.getRGB(i + 1, j + 1));
	            
	            // apply filter
	            int gradientX = ((Gx[0][0] * val00) + (Gx[0][1] * val01) + (Gx[0][2] * val02)
	            		+ (Gx[1][0] * val10) + (Gx[1][1] * val11) + (Gx[1][2] * val12)
	            		+ (Gx[2][0] * val20) + (Gx[2][1] * val21) + (Gx[2][2] * val22));
	            int gradientY = ((Gy[0][0] * val00) + (Gy[0][1] * val01) + (Gy[0][2] * val02)
	            		+ (Gy[1][0] * val10) + (Gy[1][1] * val11) + (Gy[1][2] * val12)
	            		+ (Gy[2][0] * val20) + (Gy[2][1] * val21) + (Gy[2][2] * val22));
	          
	            int valResult = (int) Math.sqrt((gradientX * gradientX)+(gradientY * gradientY));
	            // Update Normalization value
	            if(maxGradient < valResult) {
	            	maxGradient = valResult;
	            }
	            
	            edgeColors[i][j]=valResult;
			}			
		}
		// calculate scale factor so that remains between 0 y 255
		double scale = 255.0 / maxGradient;
		// obtained results to gray scale
		for (int i = 1; i < width-1; i++) {
			for (int j = 1; j < height-1; j++) {
				int edgeColor = edgeColors[i][j];
				// Normalization
                edgeColor = (int)(edgeColor * scale);
                //rgb 
                edgeColor = 0xff000000 | (edgeColor << 16) | (edgeColor << 8) | edgeColor;
                img.setRGB(i, j, edgeColor);
			}
			
		}
		return new SerializableImage(img, image.getFormat());
	}


    /**
     * Functions that converts an rgb pixel into a gray value.
     * @param rgb pixel in rgb
     * @return gray value
     */
	private static int getGrayValue(int rgb) {
		int red= (rgb >> 16) & 0xff;
		int green = (rgb >> 8) & 0xff;
		int blue = rgb & 0xff;
		
		return (int) (0.2126 * red + 0.7152 * green + 0.0722 * blue);
	}
}