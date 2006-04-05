package trycatch_in;

public class TestThrowInCatch {
	void f() throws NumberFormatException {
		/*[*/try{
		} catch (Exception e) {
			throw new Exception();
		}/*]*/
	}
}
