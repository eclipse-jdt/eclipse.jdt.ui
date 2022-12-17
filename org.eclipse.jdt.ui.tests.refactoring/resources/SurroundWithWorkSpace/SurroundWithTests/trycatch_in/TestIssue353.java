package trycatch_in;

public class TestIssue353 {
	public void foo() {
		/*[*/throw new Exception();/*]*/
		try {
			
		} catch (Exception e) {
			// do nothing
		}
	}
}
