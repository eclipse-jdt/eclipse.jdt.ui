import static java.lang.Math.E;

public class Inner
{
	/** Comment */
	private A a;

	Inner(A a) {
		this.a= a;
		int f= this.a.foo();
		int g= this.a.bar();
		double e= E;
	}
}