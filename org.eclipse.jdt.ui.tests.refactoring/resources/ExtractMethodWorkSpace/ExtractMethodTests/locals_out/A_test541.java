package locals_out;

public class A_test541 {
	class Inner {
		public int x;
	}
	public void foo() {
		int[] array= new int[10];
		Inner inner= new Inner();
		
		extracted(array, inner);
	}
	protected void extracted(int[] array, Inner inner) {
		/*[*/array[inner.x]= 10;
		inner.x= 20;/*]*/
	}
}
