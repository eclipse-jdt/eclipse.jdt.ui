package invalidSelection;

public class A_test062 {
	public void foo() {
		/*]*/do 
			foo();/*[*/
		while(1 < 10);
	}
}