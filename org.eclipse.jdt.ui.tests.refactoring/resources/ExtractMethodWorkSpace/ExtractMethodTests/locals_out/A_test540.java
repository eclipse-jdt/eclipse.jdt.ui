package locals_out;

public class A_test540 {
	public void foo() {
		int i= 0;
		int[] array= new int[10];
		int[] index= new int[1];
		
		extracted(array, index);
	}

	protected void extracted(int[] array, int[] index) {
		/*[*/array[0]= index[0];/*]*/
	}
}
