package generic_in;

public class TestMethodInstance2 {
	String bar() {
		return /*]*/foo()/*[*/;
	}
	private <T> T foo() {
		T t= null;
		return t;
	}
}