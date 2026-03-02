package p;
// Class A
public final class A {

	private final int a;
	private final String b;
	private static int c;

	static {
		c= 4;
	}

	public A(int a, String b) {
		this.a= a;
		this.b= b;
	}

	public static int getC() {
		return c;
	}

	public int getA() {
		return a;
	}

	public String getB() {
		return b;
	}
}