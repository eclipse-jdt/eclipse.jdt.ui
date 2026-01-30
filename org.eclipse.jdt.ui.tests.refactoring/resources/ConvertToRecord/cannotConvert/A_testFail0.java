package p;
// Class A
public class A {

	/**
	 * Inner
	 */
	static class Inner {
		private final int a;
		private final String b;

		public Inner(int a, String b) {
			this.a= a;
			this.b= b;
		}
		
		public Inner(int a) {
			this.a = a;
			this.b = "234";
		}

		public int getA() {
			return a;
		}

		public String getB() {
			return b;
		}
	}
}