package locals_in;

public class A_test508 {
	public void foo() {
		int x= 0;
		int y= 0;
		
		/*[*/x= 10;
		y= 20;/*]*/
		
		y= x;
	}	
}