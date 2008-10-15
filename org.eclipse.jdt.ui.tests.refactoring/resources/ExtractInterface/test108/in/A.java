package p;

public class A {
	public void m() {
		for (A a : getCollection()) {
			a.abc();
		}
		for (A a : getCollection()) {

		}
	}

	private void abc() {
	}
}
