package locals_in;

public class A_test562 {
	public boolean flag;
	public void foo() {
		int x= 0;
		do {
			int y= x;
			/*[*/x= 20;/*]*/
		} while (true);
	}
}

