package return_out;

public class A_test721 {
	public volatile boolean flag;
	
	public void foo() {
		int i;
		i = extracted();
		if (flag)
			i= 20;
		i--;
	}

	protected int extracted() {
		int i;
		/*[*/i= 10;/*]*/
		return i;
	}

}

