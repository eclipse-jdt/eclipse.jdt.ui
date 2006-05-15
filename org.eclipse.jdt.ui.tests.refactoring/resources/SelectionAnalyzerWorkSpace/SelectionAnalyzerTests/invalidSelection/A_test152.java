package invalidSelection;

public class A_test152 {

	public void foo() {
		synchronized (this) /*]*/{
			foo();
		/*]*/}
	}
}
