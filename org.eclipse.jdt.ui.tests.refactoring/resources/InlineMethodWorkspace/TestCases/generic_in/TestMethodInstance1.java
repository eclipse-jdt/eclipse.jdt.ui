package generic_in;

public class TestMethodInstance1 {
	void bar() {
		/*]*/foo("Eclipse");/*[*/
	}
	private <T> void foo(T param) {
		T t= null;
	}
}