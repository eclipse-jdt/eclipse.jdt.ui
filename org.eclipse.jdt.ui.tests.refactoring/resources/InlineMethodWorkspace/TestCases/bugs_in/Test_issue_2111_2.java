package bugs_in;

public class Test_issue_2111_2 {
	public interface A {
		void doSomething(int a, int b);
	}

	protected void foo() {
		A a = (m, n) -> b(m, n);
	}

	public void /*]*/b/*[*/(int x, int y) {
		System.out.println(x);
		System.out.println(y);
	}
}
