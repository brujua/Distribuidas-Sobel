package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteSobel extends Remote {

	SerializableImage process(SerializableImage img) throws Exception;

}
