package branch_in;

public class A_test755 {
	public volatile boolean flag;

	public void foo() {
		/*[*/do {
			if (flag)
				break;
		} while (flag);/*]*/
	}
}

