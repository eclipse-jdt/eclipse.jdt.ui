package bugs_in;

public class Test_issue_2111_1 {
	public interface A {
		void doSomething(int a, int b);
	}

	protected void foo() {
		A t = this::b;
	}

	public void /*]*/b/*[*/(int x, int y) {
	}
}
