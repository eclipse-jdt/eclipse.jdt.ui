package bugs_in;

public class Test_issue_2111_2 {
	protected void foo() {
		Runnable t = () -> {};
	}
}
