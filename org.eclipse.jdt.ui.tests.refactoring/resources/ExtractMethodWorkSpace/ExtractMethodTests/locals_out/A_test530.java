package locals_out;

public class A_test530 {
	class Inner {
		public int x;
	}
	
	public void foo() {
		Inner inner= null;
		
		inner = extracted();
		
		inner.x= 10;
	}

	protected Inner extracted() {
		Inner inner;
		/*[*/inner= new Inner();/*]*/
		return inner;
	}
}