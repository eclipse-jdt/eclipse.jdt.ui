package locals_out;

public class A_test533 {
	class Inner {
		public int x;
	}
	
	public void foo() {
		Inner inner = extracted();

		Inner inner2= inner;		
	}

	protected Inner extracted() {
		/*[*/Inner inner= new Inner();/*]*/
		return inner;
	}
}