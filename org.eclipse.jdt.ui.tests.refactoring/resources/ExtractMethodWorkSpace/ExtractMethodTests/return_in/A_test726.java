package return_in;

public class A_test726 {

	boolean flag;

	protected void foo() {
		int j= 0;
		for(int i= 0; i < 10; i++) {
			if (flag) {
				/*[*/j= 10;/*]*/
			} else {
				read(j);
			}
		}
	}

	private void read(int i) {
	}
}
