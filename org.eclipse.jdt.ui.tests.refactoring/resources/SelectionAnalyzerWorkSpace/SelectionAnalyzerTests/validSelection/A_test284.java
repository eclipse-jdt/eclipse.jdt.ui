package validSelection;

public class A_test284 {
	public boolean fBoolean;
	public void foo() {
		if (fBoolean)
			foo();
			
		if (fBoolean) {
		} else {
			/*]*/foo();/*[*/
		}
	}
}