package validSelection;

public class A_test309 {
	public void foo() {
		/*]*/try {
			foo();
		} catch (Exception e) {
			foo();
		} finally {
			foo();
		}/*[*/
	}
}