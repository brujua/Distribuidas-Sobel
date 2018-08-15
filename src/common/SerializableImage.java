package common;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.imageio.ImageIO;

public class SerializableImage implements Serializable {

	private String format;
	private static final long serialVersionUID = 1L;
	private transient BufferedImage img;
	
	
	public SerializableImage(BufferedImage image, String format) {
		this.img = image;
		this.format=format;
	}
	

	public BufferedImage getImg() {
		return img;
	}

	public void setImg(BufferedImage img) {
		this.img = img;
	}
	
	public String getFormat() {
		return format;
	}
	
	//methods to serialize the image
	private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();        
        ImageIO.write(img, format, out);
    
    }
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();       
        img=ImageIO.read(in);
        
    }
}
