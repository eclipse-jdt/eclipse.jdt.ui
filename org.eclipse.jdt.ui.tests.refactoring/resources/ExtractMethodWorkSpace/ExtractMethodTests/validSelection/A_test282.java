package validSelection;

public class A_test282 {
	public boolean fBoolean;
	public void foo() {
		if (fBoolean)/*[*/
			foo();
		/*]*/else
			foo();
	}
}