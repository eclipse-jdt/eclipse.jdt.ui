package validSelection;

public class A_test283 {
	public boolean fBoolean;
	public void foo() {
		if (fBoolean) {
		} else {
			/*]*/foo();/*[*/
		}
	}
}