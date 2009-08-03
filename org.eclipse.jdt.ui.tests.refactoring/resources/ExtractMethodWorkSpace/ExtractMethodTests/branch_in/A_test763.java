package branch_in;

public class A_test763 {

	public void foo() {
		inner: for (int i= 0; i < 3; i++) {
			/*[*/
			if(i == 2) {
				continue inner;
			}
			System.out.println();
			/*]*/
		}
	}
}

