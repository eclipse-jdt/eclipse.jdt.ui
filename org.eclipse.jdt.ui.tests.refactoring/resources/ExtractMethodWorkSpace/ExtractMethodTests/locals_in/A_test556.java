package locals_in;

public class A_test556 {
	public boolean flag;
	public void foo() {
		int x= 0;
		while (true) {
			for (int y= 0; true; y= x) {
				/*[*/x= 20;/*]*/
			}
		}
	}
}

