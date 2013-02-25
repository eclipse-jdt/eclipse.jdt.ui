package invalidSelection;

public class A_test153 {

	public void foo() {
		synchronized(this) {/*[*/
			foo();
		}/*[*/
	}
}
