package p;
// Class A
public class A {

	private class Base {
		
	}
	/**
	 * Inner
	 */
	static class Inner extends Base {
		private int a;
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