package trycatch_out;

public class TestRuntimeException1 {
	public void foo() {
		Class clazz= null;
		
		try {
			/*[*/clazz.getConstructors();/*]*/
		} catch (SecurityException e) {
		}
	}
}
