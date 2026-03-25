package p;
// Class A
public record A(int a, String b, int c) {
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
}