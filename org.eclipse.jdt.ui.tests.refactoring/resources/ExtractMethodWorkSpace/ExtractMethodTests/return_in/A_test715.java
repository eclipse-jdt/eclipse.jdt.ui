package return_in;

public class A_test715 {
	public interface I {
		public boolean run();
	}
	public void foo() {
		/*[*/bar (this, new I() {
			public boolean run() {
				return true;
			}
		});/*]*/
	}
	public void bar(A_test715 a, I i) {
	}
}
