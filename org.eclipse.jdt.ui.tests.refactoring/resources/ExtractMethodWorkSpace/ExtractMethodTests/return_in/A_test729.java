package return_in;

public class A_test729 {
	public void foo(int x) {
		while (x==3) {
		}
		int a = 0;
		for (int i = 0; i < 3; i++) {
			/*[*/g(i++);/*]*/
		}
	}

	private void g(int i) {}
}
