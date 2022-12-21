package trycatch_in;

public class TestIssue353 {
	public void foo() {
		try {
			/*[*/throw new Exception();/*]*/
		} catch (Exception e) {
		}
		try {
			
		} catch (Exception e) {
			// do nothing
		}
	}
}
