package validSelection;

public class A_test303 {
	public void foo() {
		try {
			foo();
		} catch (Exception e) {
			/*]*/foo()/*[*/;
		}
	}
}