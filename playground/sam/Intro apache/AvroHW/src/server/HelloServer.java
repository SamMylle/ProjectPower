package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.avro.AvroRemoteException ;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;
import avro.hello.proto.Hello;

public class HelloServer implements Hello{
		private int id = 0;
		@Override
		public CharSequence sayHello(CharSequence username) throws AvroRemoteException{
			System.out.println("Client connected: " + username + " (number:" + id +	")") ;
			return "Hello" + username + ", you are user number " + id++;
		}
		public static void main(String[] args) {
			Server server = null;
			try{
				server = new SaslSocketServer(new SpecificResponder(Hello.class, new HelloServer()), new InetSocketAddress(6789));
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

}
