package locals_out;

public class A_test539 {
	public void foo() {
		int i= 0;
		int[] array= new int[10];
		int[] index= new int[1];
		
		extracted(i, array, index);
	}

	protected void extracted(int i, int[] array, int[] index) {
		/*[*/array[index[i]]= 10;/*]*/
	}
}
