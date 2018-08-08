package nodos;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


//Clase encargada de instanciar los workers y registrarlos
public class WorkerManager {
	
	public static final int NUMBER_OF_WORKERS = 4;
	
	public static void main(String[] args) {
		try {
			Registry registry = LocateRegistry.createRegistry(5002);
			for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
				SobelWorker worker = new SobelWorker();
				//RemoteSobel stub = (RemoteSobel) UnicastRemoteObject.exportObject(worker, 0);
				registry.rebind("sobel"+i, worker);
				System.out.println("sobelWorker"+i+" se encuentra conectado y listo para trabajar");
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
