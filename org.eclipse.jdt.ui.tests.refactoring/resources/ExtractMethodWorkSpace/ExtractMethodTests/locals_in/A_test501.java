package locals_in;

public class A_test501 {
	public void foo() {
		int x= 10;
		
		/*[*/x= 20;
		int y= x;/*]*/
	}
}