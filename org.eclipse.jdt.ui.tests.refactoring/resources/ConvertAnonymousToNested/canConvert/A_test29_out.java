package p;
class A {
	private final class Inner implements Runnable {
		public void run() {
			fTarget.intValue();
		}
	}

	private final Integer fTarget= null;

	void test() {
		new Inner();
	}
}
