package return_in;

public class A_test731 {
	public void foo(int x) {
		for (int i = 0; i < 3; i++) {
		}
		
		int a = 0;
		do {
			/*[*/g(a++);/*]*/
		} while (x==3);
	}

	private void g(int i) {}
}

