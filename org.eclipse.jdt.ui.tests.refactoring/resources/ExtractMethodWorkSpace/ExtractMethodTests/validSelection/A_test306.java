package validSelection;

public class A_test306 {
	public void foo() {
		try {
			foo();
		} catch (Exception e) {
			foo();
		} catch (Error e) {
			/*]*/foo();/*[*/
		} catch (Throwable t) {
			foo();
		}
	}
}