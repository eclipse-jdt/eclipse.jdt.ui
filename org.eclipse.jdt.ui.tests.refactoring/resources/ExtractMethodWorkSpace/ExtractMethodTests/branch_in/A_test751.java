package branch_in;

public class A_test751 {
	public volatile boolean flag;

	public void foo() {
		/*[*/for (int i= 0; i < 10; i++) {
			if (flag)
				break;
		}/*]*/
	}
}

