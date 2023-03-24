package p; // 5, 18, 5, 35

public class A {
	void foo(C c,C c2) {
		int v1 = c.getObj().getX();
		c.getObj().setX(0);
		int v2 = c.getObj().getX();
		c.getObj().setX(3);
		int v3 = c.getObj().getX();
		c.setObj(c);
		int v4 = c.getObj().getX();
		c = new C();
		int v5 = c.getObj().getX();
		c2.setObj(c);
		int v6 = c.getObj().getX();
	}
}

class B {
	int x;

	public int getX() {
		return x;
	}

	public int setX(int x) {
		this.x = x;
		return x;
	}

}

class C extends B {
	B obj;

	public B getObj() {
		return obj;
	}

	public void setObj(B obj) {
		this.obj = obj;
	}

}