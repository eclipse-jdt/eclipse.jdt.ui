package invalidSelection;

public class A_test082 {
	public boolean fBoolean;
	public void foo() {
		/*]*/if (fBoolean)
			foo();/*[*/
		else
			foo();
	}
}