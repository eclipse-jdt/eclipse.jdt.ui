package return_in;

public class A_test706 {
	public boolean foo() {
		/*[*/try {
			foo();
			return true;
		} catch(Exception e) {
			return false;
		}/*]*/
	}
}
