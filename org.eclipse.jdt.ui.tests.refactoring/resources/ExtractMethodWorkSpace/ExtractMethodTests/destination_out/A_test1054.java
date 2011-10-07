package destination_in;

public class A_test1054 {
	public class Inner {
		public void extracted() {
			
		}
		public int foo() {
			return A_test1054.this.extracted();
		}
	}

	protected int extracted() {
		return /*[*/2 + 3/*]*/;
	}
	
}
