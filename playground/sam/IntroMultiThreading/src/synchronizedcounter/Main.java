package synchronizedcounter;

import java.util.ArrayList;
import java.util.List;

public class Main {

	public static void main(String[] args) {
		SynchronizedCounter counter = new SynchronizedCounter(0);

		long countLimit = 100000000;
		int threadCount = 15;

		// Print the expected result from the code
		System.out.println(countLimit * threadCount);
		List<Thread> threads = new ArrayList<Thread>();

		for (int i = 0; i < threadCount; i++) {
			Thread t = new Thread(new Incrementor(counter, countLimit));
			threads.add(t);
			t.start();
		}
		for (int i = 0; i < threadCount; i++) {
			try {
				threads.get(i).join();
			} catch (InterruptedException ex) {
			}
		}
		// Print the obtained result
		System.out.println(counter);
	}
}
