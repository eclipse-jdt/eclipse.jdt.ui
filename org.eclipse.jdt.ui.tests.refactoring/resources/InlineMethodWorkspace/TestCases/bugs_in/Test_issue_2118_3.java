package bugs_in;

public class Test_issue_2118_3 {
	public interface A {
		void doSomething(int a, int b);
	}

	protected void foo() {
		A a = this::b;
	}

	public void /*]*/b/*[*/(int x, int y) {
		System.out.println(x);
		System.out.println(y);
	}
}
