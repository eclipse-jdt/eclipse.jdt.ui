package bugs_in;

public class Test_issue_2111_1 {
	protected void a() {
		new Thread(() -> /*]*/b()/*[*/);
	}

	private void b() {}
}
