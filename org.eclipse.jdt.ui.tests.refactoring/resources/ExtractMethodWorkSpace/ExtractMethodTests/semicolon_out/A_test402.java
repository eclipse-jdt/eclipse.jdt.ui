package semicolon_out;

public class A_test402 {
	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/{
			foo();
		}/*]*/
	} 
}
