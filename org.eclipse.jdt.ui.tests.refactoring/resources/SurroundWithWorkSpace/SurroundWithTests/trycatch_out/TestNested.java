package trycatch_out;

public class TestNested {
	public void foo() {
		try {
			throw new Exception();
		} catch (Exception e) {
			try {
				/*[*/throw new Exception();/*]*/
			} catch (Exception e1) {
			}
		}
	}
}
