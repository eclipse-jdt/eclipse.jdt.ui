package semicolon_out;

public class A_test403 {
	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/try {
			foo();
		} catch (Exception e) {
			foo();
		}/*]*/
	} 
}
