package nodos;

import common.EstadoNodo;

public class WorkerThread implements Runnable {

	private static final String ERR_MSG_BUSY = "Already working";
	private EstadoNodo estado;
	//private ExceptionListener expListener;
	public WorkerThread() {
		super();
		this.estado = EstadoNodo.DISPONIBLE;
	}
	
	@Override
	public void run(){
		if(this.estado==EstadoNodo.TRABAJANDO) {
			System.out.println(ERR_MSG_BUSY);
		}
		synchronized(this.estado) {
			this.estado=EstadoNodo.TRABAJANDO;
		}
		
		
		// TODO Auto-generated method stub

	}

}
