public class A {
	public interface I {
		public boolean run();
	}
	public void foo() {
		bar (this, new I() {
			public boolean run() {
				return true;
			}
		});
	}
	public void bar(A a, I i) {
	}
}

