package return_out;

public class A_test705 {
	public boolean foo() {
		return extracted();
	}

	protected boolean extracted() {
		/*[*/try {
			foo();
		} catch(Exception e) {
		} finally {
			return false;
		}/*]*/
	}
}
