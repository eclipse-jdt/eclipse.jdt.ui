package branch_in;

public class A_test753 {
	public volatile boolean flag;

	public void foo() {
		/*[*/while (flag) {
			if (flag)
				break;
		}/*]*/
	}
}

