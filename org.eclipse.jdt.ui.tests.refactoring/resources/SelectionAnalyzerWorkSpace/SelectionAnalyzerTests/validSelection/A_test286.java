package validSelection;

public class A_test286 {
	public boolean fBoolean;
	public void foo() {
		/*[*/foo();/*]*/	
		if (fBoolean)
			foo();
	}
}