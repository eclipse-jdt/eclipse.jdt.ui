package invalidSelection;

public class A_test022 {
	public void foo(int x) {
		switch(x) {
			/*]*/case 10:
				f();
				break;
			case 11:
				g();
				break;
			default:
				f();
				g();		
		/*]*/}
	}
	
	public void f() {
	}
	public void g() {
	}
}