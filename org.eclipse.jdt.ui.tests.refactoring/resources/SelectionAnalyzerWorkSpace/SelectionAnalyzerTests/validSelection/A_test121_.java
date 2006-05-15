package validSelection;

public class A_test121_ {
	public void foo() {
		int x= 1;
		int y= 2;
		
		/*]*/x= y + x;
		y= x + y;/*[*/
		
		x++;
		y++;
	}
}