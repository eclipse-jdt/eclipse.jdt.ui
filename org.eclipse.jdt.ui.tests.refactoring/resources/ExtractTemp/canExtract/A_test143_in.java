package p; // 6, 16, 6, 24

public class A {
	int foo(int v) {
		B b = new B(1);
		return b.getX() + b.setX(v) + b.getX();
	}
}

class B {
	int x;

	public int getX() {
		return x;
	}

	public int setX(int v) {
		return this.x = v;
	}

	public B(int x) {
		super();
		this.x = x;
	}

}