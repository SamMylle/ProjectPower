package threading;

public class Greeter implements Runnable {

	String[] greetings = { "Hallo!", "Guten Tag!", "Bonjour!", "Hello!" };

	public void run() {
		for (int i = 0; i < greetings.length; i++) {
			System.out.println(greetings[i]);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				System.out.print("exep");
				return;
			}
		}
	}

	public static void main(String[] args) {
		Thread t = new Thread(new Greeter());
		Thread t2 = new Thread(new Greeter());
		t.start();
		t2.start();
		try {
			t.join();
			t2.join();
		} catch (InterruptedException e) {
			System.out.print("exep");
			return;
		}
		System.out.println("Greeter completed!");
	}
}
