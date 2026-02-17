package p;
// Class A
public class A {

	/**
	 * Inner
	 */
	private static class Inner extends Object {
		private final int a;
		private final String b;

		public Inner(int a, String b) {
			this.a= a;
			this.b= b;
		}

		public int aValue() {
			return a;
		}

		public String bValue() {
			return b;
		}
	}
	
	public void foo() {
		Inner x= new Inner(3, "abc");
		System.out.println(x.aValue());
		System.out.println(x.bValue());
	}
}