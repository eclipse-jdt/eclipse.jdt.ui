package semicolon_out;

public class A_test411 {
	int field = 0;
	
	public void foo() {
		/*]*/extracted();
	}

	protected void extracted() {
		field = 1;/*]*/
	} 
}