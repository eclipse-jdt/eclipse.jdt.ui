package invalidSelection;

public class A_test140 {
	public void foo() {
		synchronized(this) {
			foo();
		}
		
		foo();
	}
}