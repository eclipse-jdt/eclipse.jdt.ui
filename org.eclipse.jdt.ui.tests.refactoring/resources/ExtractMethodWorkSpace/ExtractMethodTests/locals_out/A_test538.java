package locals_out;
public class A_test538 {
	public void foo() {
		int i= 0;
		int[] array= new int[10];
		
		/*]*/extracted(array, i);/*[*/
	}
	protected void extracted(int[] array, int i) {
		array[i]= 10;
	}
}
