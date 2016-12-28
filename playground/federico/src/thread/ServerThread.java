package thread;


import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;

import avro.ProjectPower.ControllerComm;


public class ServerThread implements Runnable{
	public Server server;
	private int f_port;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try{
			server = new SaslSocketServer(
					new SpecificResponder(ControllerComm.class,
					this), new InetSocketAddress(f_port));
		}catch(IOException e){
			System.err.println("[error]Failed to start server");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		server.start();
		try{
			server.join();
		}catch(InterruptedException e){}
	}

	public void setPort(int port){
		f_port = port;
	}
	
	public void stopServer(){
		if (server != null){
			server.close();
		}
	}
}
