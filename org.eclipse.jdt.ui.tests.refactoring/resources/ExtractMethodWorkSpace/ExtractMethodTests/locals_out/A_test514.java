package locals_out;
public class A_test514 {
	public void foo() {
		int i= 0;
		int[] array= new int[10];
		int[] index= new int[1];
		
		/*]*/extracted(array, index, i);/*[*/
	}
	protected void extracted(int[] array, int[] index, int i) {
		array[index[i]]= 10;
	}
}
