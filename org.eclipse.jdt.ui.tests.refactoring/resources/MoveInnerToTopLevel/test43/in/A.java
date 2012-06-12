package p;
public class A {
	protected class B {
		public void execute() {
			synchronized (B.this) {
				System.err.println();
			}
		}
	}
}