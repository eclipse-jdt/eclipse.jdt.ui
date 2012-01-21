package semicolon_out;

public class A_test412 {
	int field = 0;
	
	public void foo() {
		/*]*/extracted();
	}

	protected int extracted() {
		return field = 1/*]*/;
	} 
}