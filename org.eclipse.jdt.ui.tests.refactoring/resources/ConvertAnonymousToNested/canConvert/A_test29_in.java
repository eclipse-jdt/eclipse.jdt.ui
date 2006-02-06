package p;
class A {
	private final Integer fTarget= null;

	void test() {
		new Runnable() {
			public void run() {
				fTarget.intValue();
			}
		};
	}
}
