package branch_in;

public class A_test754 {
	public volatile boolean flag;

	public void foo() {
		/*[*/do {
			if (flag)
				continue;
		} while (flag);/*]*/
	}
}

