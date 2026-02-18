package p;
// Class A
public class A {

	/**
	 * Inner
	 */
	static class Inner {
		public int a;
		private final String b;

		public Inner(int a, String b) {
			this.a= a;
			this.b= b;
		}
		
		public int getA() {
			return a;
		}

		public String getB() {
			return b;
		}
	}
}