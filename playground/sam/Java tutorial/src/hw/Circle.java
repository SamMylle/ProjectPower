package hw;

public class Circle{
	protected double radius = 0;
	public Circle(){} // d e f a u l t c o n s t r u c t o r
	public Circle(double r){ // c o n s t r u c t o r
		radius = r;
	}
	public Circle(Circle c){ // copy−c o n s t r u c t o r
		this(c.getRadius());
	}
	public double getRadius(){ // a c c e s s o r
		return radius;
	}
	public void changeRadius(int r){ // m o d i f i e r
		radius = r;
	}
	public double calculateGirth(){
		return 2.0 * radius * Math.PI;
	}
	public double calculateArea(){
		return radius * radius * Math.PI;
	}
	@Override
	public String toString(){ // Re t u r n c i r c l e i n f o
		return "Circle:\n\tradius = " + radius + "\n";
	}
}
