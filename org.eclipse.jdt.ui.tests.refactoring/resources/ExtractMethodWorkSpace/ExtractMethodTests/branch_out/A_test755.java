package branch_out;

public class A_test755 {
	public volatile boolean flag;

	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/do {
			if (flag)
				break;
		} while (flag);/*]*/
	}
}

