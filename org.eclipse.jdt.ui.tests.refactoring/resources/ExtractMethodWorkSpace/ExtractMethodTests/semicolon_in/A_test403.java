package semicolon_in;

public class A_test403 {
	public void foo() {
		/*[*/try {
			foo();
		} catch (Exception e) {
			foo();
		}/*]*/
	} 
}
