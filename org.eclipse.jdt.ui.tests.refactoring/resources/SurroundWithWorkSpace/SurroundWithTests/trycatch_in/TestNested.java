package trycatch_in;

public class TestNested {
	public void foo() {
		try {
			throw new Exception();
		} catch (Exception e) {
			/*[*/throw new Exception();/*]*/
		}
	}
}
