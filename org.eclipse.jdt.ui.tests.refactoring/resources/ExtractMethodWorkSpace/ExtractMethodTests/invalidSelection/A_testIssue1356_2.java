package invalidSelection;

public class A_testIssue1356_2 {

	private final int fValue;
	
	public A_testIssue1356_2(int someValue) {
		/*]*/fValue= someValue;/*[*/
	}

	public void foo(int a) {
        if (this.fValue < 3) {
            return;
        }
        System.out.println(a);
    }

}