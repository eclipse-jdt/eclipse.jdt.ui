package branch_out;

public class A_test752 {
	public volatile boolean flag;

	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/while (flag) {
			if (flag)
				continue;
		}/*]*/
	}
}

