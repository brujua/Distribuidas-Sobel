package nodes;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import common.RemoteSobel;


//Clase encargada de instanciar los workers y registrarlos
public class WorkerManager {
	
	//config
	public static final int NUMBER_OF_WORKERS = 4;
	public static final String ip = "192.168.0.40";
	public static final int port = 5020;
	
	public static void main(String[] args) {
		try {
			System.setProperty("java.rmi.server.hostname",ip);
			Registry registry = LocateRegistry.createRegistry(port);			
			for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
				SobelWorker worker = new SobelWorker();
				RemoteSobel stub = (RemoteSobel) UnicastRemoteObject.exportObject(worker, 0);
				registry.rebind("sobel"+i, stub);
				System.out.println("sobelWorker"+i+" se encuentra conectado y listo para trabajar");
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
