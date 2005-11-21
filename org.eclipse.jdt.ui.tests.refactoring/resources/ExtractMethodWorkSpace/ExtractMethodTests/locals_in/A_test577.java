package locals_in;

public class A_test577 {
	public void foo() {
		int x = 0;
		for (int i = x; i < 10; i++)
			/*[*/
			bar(i, x++);
			/*]*/
	}
	
	private void bar(int i, int y) {
	}
}

