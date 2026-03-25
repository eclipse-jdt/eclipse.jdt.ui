package p;
// Class A
public final class A {

	private final int a;
	private final String b;
	private int c;

	public A(int a, String b, int c) {
		class K {
			public static int doublex(int x) {
				return x * 2;
			}
		}
		this.a= K.doublex(a);
		if (a < 0) {
			this.b = massage(b);
		} else {
			this.b = b;
		}
		this.c= c;
	}

	private String massage(String s) {
		return s.toLowerCase();
	}

	public int getA() {
		return a;
	}

	public String getB() {
		return b;
	}

	public int getC() {
		return c;
	}
}