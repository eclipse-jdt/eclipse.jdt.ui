package return_in;

public class A_test720 {
	private boolean flag;
	public boolean foo() {
		if (flag) {
			/*[*/try {
				foo();
			} catch(Exception e) {
			}
			return false;/*]*/
		}
		return true;
	}
}

