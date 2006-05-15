package validSelection;

public class A_test285 {
	public boolean fBoolean;
	public void foo() {
		if (fBoolean)
			/*[*/foo();/*]*/
			
		if (fBoolean) {
		} else {
			foo();
		}
	}
}