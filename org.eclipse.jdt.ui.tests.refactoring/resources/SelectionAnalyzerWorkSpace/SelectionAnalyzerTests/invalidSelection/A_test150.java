package invalidSelection;

public class A_test150 {

	public void foo() {
		/*]*/synchronized (this) {
			foo();
		/*]*/}
	}
}
