package locals_in;

public class A_test534 {
	class Inner {
		public int x;
	}
	
	public void foo() {
		Inner inner= new Inner();
		
		/*[*/inner.x= 10;/*]*/
		
		int y= inner.x;
	}
}