package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.CallFuture;
import avro.hello.proto.Hello;

public class HelloClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(6789));
			Hello.Callback proxy = SpecificRequestor.getClient(Hello.Callback.class,client);
			CallFuture<CharSequence> future = new CallFuture<CharSequence>(); 
			proxy.sayHello("Bob", future);
			System.out.println(future.get());
			client.close();
		}catch(ExecutionException e){
			System.err.println("Error executing command on server...");
			e.printStackTrace(System.err);
			System.exit(1);
		}catch(InterruptedException e){
			System.err.println("Interrupted...");
			e.printStackTrace(System.err);
			System.exit(1);
		}catch(IOException e){
			System.err.println("Error connecting to server...");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

}
