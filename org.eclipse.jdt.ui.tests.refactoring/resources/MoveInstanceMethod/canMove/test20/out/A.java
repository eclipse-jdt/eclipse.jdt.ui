package p;

public class A {
	public static void main(String[] args) {
		A a= new A();
		B b= new B("Gugguseli");
		a.print(b);
	}

	/**
	 * Print
	 * @param b
	 */
	public void print(B b) {
		b.print();
	}
}
