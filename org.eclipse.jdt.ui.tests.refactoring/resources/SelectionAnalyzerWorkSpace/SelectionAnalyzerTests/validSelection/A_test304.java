package validSelection;

public class A_test304 {
	public void foo() {
		try {
			foo();
		} catch (Exception e) {
			foo();
		} catch (Throwable t) {
			/*]*/foo();/*[*/
		}
	}
}