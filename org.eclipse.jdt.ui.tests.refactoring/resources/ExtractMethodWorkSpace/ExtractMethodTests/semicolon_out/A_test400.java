package semicolon_out;

public class A_test400 {
	public void foo() {
		/*]*/extracted();
	}

	protected void extracted() {
		foo()/*]*/;
	} 
}