package validSelection;

public class A_test130_ {
	public void foo() {
		class Inner {
		}
		
		/*]*/Inner inner= new Inner();
		foo();/*[*/
	}
}