package validSelection;

public class A_test301 {
	public void foo() {
		try {
			/*]*/foo()/*[*/;
		} catch (Exception e) {
			foo();
		}
	}
}