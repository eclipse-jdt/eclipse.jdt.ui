package invalidSelection;

public class A_test012 {
	public void foo() {
		/*]*/f();
		g(/*[*/);
	}
	
	public void f() {
	}
	public void g() {
	}
}