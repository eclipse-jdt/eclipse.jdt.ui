package return_in;

public class A_test705 {
	public boolean foo() {
		/*[*/try {
			foo();
		} catch(Exception e) {
		} finally {
			return false;
		}/*]*/
	}
}
