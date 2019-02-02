package p;

public class A implements I {
	public void m() {
		for (A a : getCollection()) {
			a.abc();
		}
		for (I a : getCollection()) {

		}
	}

	private void abc() {
	}
}
