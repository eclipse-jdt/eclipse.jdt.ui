package branch_out;

public class A_test769 {

	public void foo(int[] a) {
		for(int i : a) {
			extracted(i);
		}
	}

	protected void extracted(int i) {
		/*[*/
		if( i == 3 ) {
			return;
		}
		System.out.println();
		/*]*/
	}
}

