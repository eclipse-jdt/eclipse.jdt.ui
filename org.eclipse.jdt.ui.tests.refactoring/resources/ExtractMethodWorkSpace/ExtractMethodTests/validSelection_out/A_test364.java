package validSelection_out;

// http://dev.eclipse.org/bugs/show_bug.cgi?id=6680
public class A_test364 {
	public int i(){ 
		return 0;
	}
	public void m(){
		extracted();
	}
	protected void extracted() {
		/*[*/i();
		m();/*]*/
	}
}
