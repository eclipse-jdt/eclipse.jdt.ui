package invalidSelection;

public class A_test122 {
	public void foo() {
		int x= 1;

		/*]*/x= x + 2;
		int y= 10;/*[*/
		
		x+= y + 20;
	}
}