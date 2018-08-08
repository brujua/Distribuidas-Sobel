package common;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class SerializableImage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ImageIcon img;
	
	public SerializableImage(BufferedImage image) {
		this.img= new ImageIcon(image); 
	}
	

	public BufferedImage getImg() {
		BufferedImage bi = new BufferedImage(
			    img.getIconWidth(),
			    img.getIconHeight(),
			    BufferedImage.TYPE_INT_RGB);
			Graphics g = bi.createGraphics();
			// paint the Icon to the BufferedImage.
			img.paintIcon(null, g, 0,0);
			g.dispose();
			return bi;
	}

	public void setImg(BufferedImage img) {
		this.img= new ImageIcon(img);
	}
	
	/*private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();        
        ImageIO.write(img, "png", out); // png is lossless
    
    }
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();       
        img=ImageIO.read(in);
        
    }*/
	
}
