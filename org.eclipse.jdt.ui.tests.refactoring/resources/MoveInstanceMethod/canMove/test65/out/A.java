public class A extends B {
	@Override
	public void m(C c) {
		c.m();
	}
}

class B {
	public void m(C c) {
	}
}

class C {

	public void m() {
	}

}
