package invalidSelection;

public class A_test061 {
	public void foo() {
		do 
			/*]*/foo();
		while(1 < 10);/*[*/
	}
}