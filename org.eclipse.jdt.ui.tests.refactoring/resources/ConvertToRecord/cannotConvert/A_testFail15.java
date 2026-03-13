package p;
// Class A
public class A {

	private int a;
	
	public A(int a) {
		this.a= a;
	}
	
	public int getA() {
		return a;
	}

	/**
	 * Inner
	 */
	public class Inner {
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