package validSelection;

public class A_test233 {
	public void foo() {
		{
			foo();
		}
		{
			/*]*/foo()/*[*/;
		}
	}
}