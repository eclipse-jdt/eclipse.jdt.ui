package invalidSelection;

public class A_test130 {
	public void foo() {
		class Inner {
		}
		
		/*]*/Inner inner= new Inner();
		foo();/*[*/
	}
}