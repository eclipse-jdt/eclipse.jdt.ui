package invalidSelection;

public class A_test141 {
	private boolean flag;
	public int foo() {
		/*]*/while(flag)
			return 20;/*[*/
		return 10;	
	}
}