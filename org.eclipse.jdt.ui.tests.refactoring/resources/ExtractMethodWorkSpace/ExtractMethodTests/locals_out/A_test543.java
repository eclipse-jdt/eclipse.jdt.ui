package locals_out;

public class A_test543 {
	public void foo() {
		int i= 0;
		int[] array= new int[10];
		
		i = extracted(i, array);
		
		i++;
	}

	protected int extracted(int i, int[] array) {
		/*[*/array[i++]= 10;/*]*/
		return i;
	}
}
