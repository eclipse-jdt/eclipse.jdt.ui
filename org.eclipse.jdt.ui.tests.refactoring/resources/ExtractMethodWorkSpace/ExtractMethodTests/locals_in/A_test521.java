package locals_in;

public class A_test521 {
	public volatile boolean flag;
	
	public void foo() {
		int i= 5;
		/*[*/if (flag)
			i= 10;/*]*/
		i--;
	}
}

