package return_in;

public class A_test721 {
	public volatile boolean flag;
	
	public void foo() {
		int i;
		/*[*/i= 10;/*]*/
		if (flag)
			i= 20;
		i--;
	}

}

