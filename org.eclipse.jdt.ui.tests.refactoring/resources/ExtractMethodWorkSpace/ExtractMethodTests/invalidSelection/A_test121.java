package invalidSelection;

public class A_test121 {
	public void foo() {
		int x= 1;
		int y= 2;
		
		/*]*/x= y + x;
		y= x + y;/*[*/
		
		x++;
		y++;
	}
}