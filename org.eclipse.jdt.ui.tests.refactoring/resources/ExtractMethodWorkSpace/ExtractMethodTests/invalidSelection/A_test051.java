package invalidSelection;

public class A_test051 {
	public void foo() {
		/*]*/while(1 < 10)/*[*/
			foo();
	}	
}