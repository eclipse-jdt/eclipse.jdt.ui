package locals_in;

public class A_test560 {
	public boolean flag;
	public void foo() {
		int x= 0;
		while (x < 10) {
			/*[*/x= 20;/*]*/
		}
	}
}

