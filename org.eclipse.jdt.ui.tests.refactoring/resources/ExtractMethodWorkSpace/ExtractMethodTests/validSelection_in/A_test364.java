package validSelection_in;

// http://dev.eclipse.org/bugs/show_bug.cgi?id=6680
public class A_test364 {
	public int i(){ 
		return 0;
	}
	public void m(){
		/*[*/i();
		m();/*]*/
	}
}
