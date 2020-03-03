package tryresources18_in;

public class TestExceptionFilter1 {
	public static class Exception_1 extends Exception {
	}
	public static class Exception_2 extends Exception_1 {
	}
	
	public void throw1() throws Exception_1 {
	}
	
	public void throw2() throws Exception_2 {
	}
	
	public void foo() {
		/*[*/throw1();
		throw2();/*]*/
	}	
}
