package locals_in;

public class A_test555 {
	public boolean flag;
	public void foo() {
		int x= 0;
		while (true) {
			for (int y= x; true; ) {
				/*[*/x= 20;/*]*/
			}
		}
	}
}

