package return_out;

public class A_test706 {
	public boolean foo() {
		return extracted();
	}

	protected boolean extracted() {
		/*[*/try {
			foo();
			return true;
		} catch(Exception e) {
			return false;
		}/*]*/
	}
}
