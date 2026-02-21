package p;
// Class A
public class A {

	/**
	 * Inner
	 */
	static class Inner {
		private final int a;
		private final String b;
		public static int c;

		public Inner(int a, String b) {
			this.a= a;
			this.b= b;
		}

		public Inner(int a) {
			this.a = a;
			this.b = "234";
			Inner.c = 3;
		}

		public int getA() {
			return a;
		}

		public String getB() {
			return b;
		}
	}
}