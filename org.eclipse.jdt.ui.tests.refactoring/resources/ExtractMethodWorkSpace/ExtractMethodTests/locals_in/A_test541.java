package locals_in;

public class A_test541 {
	class Inner {
		public int x;
	}
	public void foo() {
		int[] array= new int[10];
		Inner inner= new Inner();
		
		/*[*/array[inner.x]= 10;
		inner.x= 20;/*]*/
	}
}
