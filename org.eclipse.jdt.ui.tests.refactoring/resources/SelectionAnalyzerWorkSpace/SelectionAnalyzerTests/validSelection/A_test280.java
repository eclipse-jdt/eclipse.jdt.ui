package validSelection;

public class A_test280 {
	public boolean fBoolean;
	public void foo() {
		/*]*/if (fBoolean)
			foo();
		else
			foo();/*[*/
	}
}