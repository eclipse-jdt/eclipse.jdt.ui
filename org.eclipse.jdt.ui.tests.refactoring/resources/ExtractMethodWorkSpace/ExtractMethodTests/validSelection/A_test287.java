package validSelection;

public class A_test287 {
	public boolean fBoolean;
	public void foo() {
		if (fBoolean)
			foo();
		/*[*/foo();/*]*/	
	}
}