package branch_in;

public class A_test752 {
	public volatile boolean flag;

	public void foo() {
		/*[*/while (flag) {
			if (flag)
				continue;
		}/*]*/
	}
}

