package locals_in;

public class A_test559 {
	public boolean flag;
	public void foo() {
		int x= 0;
		for (int y= 0; x < 10; x= 20) {
			/*[*/x= 20;/*]*/
		}
	}
}

