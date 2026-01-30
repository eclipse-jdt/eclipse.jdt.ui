package p;
// Class A
public class A {

	/**
	 * Inner
	 */
	static class Inner {
		public int a;
		private boolean b;

		public Inner(int a) {
			this.a= a;
		}
		
		public int getA() {
			return a;
		}

		public boolean getB() {
			return b;
		}
	}
}