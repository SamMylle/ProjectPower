package synchronizedcounter;

public class Incrementor implements Runnable {

	private SynchronizedCounter counter;
	private long count;

	/**
	 * Constructor with the counter to use and the number of iterations
	 * 
	 * @param counter
	 *            The Counter to use for processing
	 * @param count
	 *            The number of increment operations to perform.
	 */
	public Incrementor(SynchronizedCounter counter, long count) {
		this.counter = counter;
		this.count = count;
	}

	/**
	 * Performing the actual business logic .
	 */
	public void run() {
		for (int i = 0; i < count; i++) {
			counter.increment();
		}

		System.out.println("Incrementor finished");
	}
}
