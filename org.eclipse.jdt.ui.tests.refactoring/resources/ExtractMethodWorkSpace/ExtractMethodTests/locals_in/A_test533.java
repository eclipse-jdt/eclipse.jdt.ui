package locals_in;

public class A_test533 {
	class Inner {
		public int x;
	}
	
	public void foo() {
		/*[*/Inner inner= new Inner();/*]*/

		Inner inner2= inner;		
	}
}