package validSelection;

public class A_test080_ {
	public boolean fBoolean;
	public void foo() {
		if (/*]*/fBoolean/*[*/)
			foo();
		else
			foo();
	}
}