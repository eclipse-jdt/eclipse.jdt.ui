package p;
// Class A
public class A {

	/**
	 * Inner
	 */
	static class Inner {
		private int a;
		private final String b;

		public Inner(int a, String b) throws Exception {
			if (a < 0) {
				throw new Exception();
			}
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