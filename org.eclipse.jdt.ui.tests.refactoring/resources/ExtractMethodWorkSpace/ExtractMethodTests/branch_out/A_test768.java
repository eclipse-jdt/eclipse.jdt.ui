package branch_out;

public class A_test768 {

	public void foo() {
		int i = 0;
		do {
			extracted(i);
		} while ( i < 10 );
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

