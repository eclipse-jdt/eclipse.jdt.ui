package validSelection;

public class A_test248 {
	public void foo() {
		for (int i= 0; i < 10; i++)
			foo();
			
		/*]*/foo();/*[*/
	}
}