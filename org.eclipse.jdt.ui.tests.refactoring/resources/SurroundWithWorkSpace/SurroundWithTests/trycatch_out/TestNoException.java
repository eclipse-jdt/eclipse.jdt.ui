package trycatch_in;

public class TestNoException {
	public void foo() {
		/*]*/try {
			foo();
		} catch (Exception e) {
		}/*[*/
	}
}
