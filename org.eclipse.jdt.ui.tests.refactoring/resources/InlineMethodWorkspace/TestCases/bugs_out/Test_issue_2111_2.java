package bugs_in;

public class Test_issue_2111_2 {
	public interface A {
		void doSomething(int a, int b);
	}

	protected void foo() {
		A a = (m, n) -> {
			System.out.println(m);
			System.out.println(n);
		};
	}
}
