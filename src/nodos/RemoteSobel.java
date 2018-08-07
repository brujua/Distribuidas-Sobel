package nodos;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteSobel extends Remote {



	//Methods
	public SerializableImage procesar (SerializableImage img) throws RemoteException, Exception;
	public EstadoNodo getEstado() throws RemoteException;
	
}
