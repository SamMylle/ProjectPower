package hw;

import java.util.Vector;
import hw.Circle;

public interface Main {
	public Vector<Double> generateRandom(int amount, double maximum);
	public Vector<Circle> makeCircles(Vector<Double> radiuses);
	public void printCircles(Vector<Circle> circles);
}
