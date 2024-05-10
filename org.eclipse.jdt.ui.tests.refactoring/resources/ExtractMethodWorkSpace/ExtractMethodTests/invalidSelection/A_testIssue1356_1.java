package invalidSelection;

public class A_testIssue1356_1 {

	private final int value;
	
	public A_testIssue1356_1(int value) {
		/*]*/this.value= value;/*[*/
	}

	public void foo(int a) {
        if (this.value < 3) {
            return;
        }
        System.out.println(a);
    }

}