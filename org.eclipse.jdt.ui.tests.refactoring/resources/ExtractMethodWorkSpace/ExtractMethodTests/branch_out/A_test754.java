package branch_out;

public class A_test754 {
	public volatile boolean flag;

	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/do {
			if (flag)
				continue;
		} while (flag);/*]*/
	}
}

