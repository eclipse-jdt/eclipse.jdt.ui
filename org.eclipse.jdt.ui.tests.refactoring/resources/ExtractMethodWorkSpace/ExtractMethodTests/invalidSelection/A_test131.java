package invalidSelection;

public class A_test131 {
	public void foo() {
		
		/*]*/foo();
		class Inner {
		}
		foo();/*[*/
		
		Inner inner= new Inner();
	}
}