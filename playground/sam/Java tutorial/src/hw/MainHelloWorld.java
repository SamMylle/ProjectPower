package hw;

import java.util.Vector;
import java.util.Random;

import hw.Circle;

public class MainHelloWorld{
	public static void main(String[] args){
		System.out.println("Hello World");
		Vector<Double> doubles = generateRandom(5, 5.0);
		Vector<Circle> circles = makeCircles(doubles);
		printCircles(circles);
	}

	public static Vector<Double> generateRandom(int amount, double maximum){
		Double Dmax = new Double(maximum);
		Vector<Double> retVal = new Vector<Double>();
		for (int i = 0; i < amount; i++){
			Random rn = new Random();
			Double rnd = rn.nextDouble() * Dmax;
			if (rnd == 0){
				i--;
				continue;
			}
			retVal.add(rnd);
		}
		return retVal;
	}

	public static Vector<Circle> makeCircles(Vector<Double> radiuses){
		Vector<Circle> retVal = new Vector<Circle>();
		for (int i = 0; i < radiuses.size(); i++){
			Circle newCircle = new Circle(radiuses.elementAt(i));
			retVal.add(newCircle);
		}
		return retVal;
	}

	public static void printCircles(Vector<Circle> circles){
		for (int i = 0; i < circles.size(); i++){
			System.out.print(circles.elementAt(i).toString());
		}
	}
}
