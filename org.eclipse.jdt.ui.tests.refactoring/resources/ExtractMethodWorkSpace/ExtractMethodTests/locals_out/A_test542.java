package locals_out;

public class A_test542 {
	public void foo() {
		int i= 0;
		int[] array= new int[10];
		
		extracted(i, array);
	}

	protected void extracted(int i, int[] array) {
		/*[*/array[i++]= 10;/*]*/
	}
}
