package synchronizedcounter;

public class SynchronizedCounter {

	private long value;

	/**
	 * Constructor with the initial value of the counter
	 * 
	 * @param startValue
	 *            the initial value
	 */
	public SynchronizedCounter(long startValue) {
		this.value = startValue;
	}

	/**
	 * Increment the counter
	 */
	public synchronized void increment() {
		value++;
	}

	/**
	 * Decrement the counter
	 */
	public synchronized void decrement() {
		value--;
	}

	/**
	 * Get the current value of the counter
	 * 
	 * @return the cur rent counter value
	 */
	public synchronized long getValue() {
		return value;
	}

	/**
	 * Get the string representation of this Counter
	 * 
	 * @return a String representing this counter
	 */
	@Override
	public String toString() {
		return "" + value;
	}
}