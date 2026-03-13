package p;
// Class A
public final class A {

	private final int a;
	private final String b;

	public A(int a, String b) {
		this.a= a;
		this.b= b;
	}

	public A(int a) {
		this(a, "abc");
	}

	public int getA() {
		return a;
	}

	public String getB() {
		return b;
	}
}