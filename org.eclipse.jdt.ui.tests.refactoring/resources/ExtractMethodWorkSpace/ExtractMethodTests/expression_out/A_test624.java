package expression_out;

public class A_test624 {

	public void foo(int i, int j, int k) {

	}

	public void bar() {
		foo(100, /*[*/extracted(),/*]*/ 300);
	}

	protected int extracted() {
		return 200;
	}
}
