package trycatch_in;

public class TestRuntimeException2 {
	public void foo() {
		/*]*/try {
			throw new NullPointerException();
		} catch (Exception e) {
		}/*[*/
	}
}
