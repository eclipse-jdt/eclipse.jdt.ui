package invalidSelection;

public class A_test140 {
	public int foo() {
		/*]*/for(int i= 0; i < 10; i++)
			return 20;/*[*/
		return 10;	
	}
}