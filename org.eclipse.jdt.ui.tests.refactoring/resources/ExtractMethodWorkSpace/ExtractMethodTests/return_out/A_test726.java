package return_out;

public class A_test726 {

	boolean flag;

	protected void foo() {
		int j= 0;
		for(int i= 0; i < 10; i++) {
			if (flag) {
				j = extracted();
			} else {
				read(j);
			}
		}
	}

	protected int extracted() {
		int j;
		/*[*/j= 10;/*]*/
		return j;
	}

	private void read(int i) {
	}
}
