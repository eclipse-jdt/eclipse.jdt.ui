package locals_in;

public class A_test530 {
	class Inner {
		public int x;
	}
	
	public void foo() {
		Inner inner= null;
		
		/*[*/inner= new Inner();/*]*/
		
		inner.x= 10;
	}
}