package locals_in;

public class A_test558 {
	public boolean flag;
	public void foo() {
		int x= 0;
		for (int y= 0; (x= 20) < 10; y= x) {
			/*[*/x= 20;/*]*/
		}
	}
}

