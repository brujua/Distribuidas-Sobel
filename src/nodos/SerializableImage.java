package nodos;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.imageio.ImageIO;

public class SerializableImage implements Serializable {

	private BufferedImage img;
	
	public SerializableImage(BufferedImage image) {
		this.img=img; 
	}
	
	public SerializableImage(File file) throws IOException {
		this.img = ImageIO.read(file);
	}

	public BufferedImage getImg() {
		return img;
	}

	public void setImg(BufferedImage img) {
		this.img = img;
	}
	
}
