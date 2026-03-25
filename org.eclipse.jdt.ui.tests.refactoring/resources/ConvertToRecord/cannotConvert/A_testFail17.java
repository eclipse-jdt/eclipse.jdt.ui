package p;
// Class A
public class A {

	/**
	 * Inner
	 */
	static class Inner {
		private int a;
		private final String b;

		public Inner(int a, String b) {
			if (a < 0) {
				this.a = a;
			}
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