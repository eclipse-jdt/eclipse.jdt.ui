package locals_in;

public class A_test522 {
	public volatile boolean flag;
	
	public void foo() {
		int i= 20;
		/*[*/target: {
			if (flag)
				break target;
			i= 10;
		}/*]*/
		i--;
	}	
}

