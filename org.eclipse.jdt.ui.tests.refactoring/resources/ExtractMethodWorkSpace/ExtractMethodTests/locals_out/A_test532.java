package locals_out;

public class A_test532 {
	class Inner {
		public int x;
	}
	
	public void foo() {
		Inner inner= new Inner();
		
		extracted(inner);
		
		int y= inner.x;
	}

	protected void extracted(Inner inner) {
		/*[*/inner.x= 10;/*]*/
	}
}