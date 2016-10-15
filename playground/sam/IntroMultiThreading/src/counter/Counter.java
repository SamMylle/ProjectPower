package counter;

public class Counter {

	private long value;

	/**
	 * Constructor with the initial value of the counter
	 * 
	 * @param startValue
	 *            the initial value
	 */
	public Counter(long startValue) {
		this.value = startValue;
	}

	/**
	 * Increment the counter
	 */
	public void increment() {
		value++;
	}

	/**
	 * Decrement the counter
	 */
	public void decrement() {
		value--;
	}

	/**
	 * Get the cur rent value of the counter
	 * 
	 * @return the cur rent counter value
	 */
	public long getValue() {
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