package locals_out;

public class A_test522 {
	public volatile boolean flag;
	
	public void foo() {
		int i= 20;
		i = extracted(i);
		i--;
	}

	protected int extracted(int i) {
		/*[*/target: {
			if (flag)
				break target;
			i= 10;
		}/*]*/
		return i;
	}	
}

