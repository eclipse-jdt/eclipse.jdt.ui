package p;
public class A {
	protected class B {
		public void execute() {
			B b = p.A.B.this;
			synchronized (A.this) {
				System.err.println();
			}
		}
	}
}
