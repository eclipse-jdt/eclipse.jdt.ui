package p;
class B {
	public void execute() {
		synchronized (B.this) {
			System.err.println();
		}
	}
}