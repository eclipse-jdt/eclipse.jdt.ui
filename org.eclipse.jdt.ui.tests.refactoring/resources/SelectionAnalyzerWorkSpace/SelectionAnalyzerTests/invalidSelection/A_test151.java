package invalidSelection;

public class A_test151 {

	public void foo() {
		synchronized/*[*/ (this) {
			foo();
		}/*[*/
	}
}
