package p; // 5, 18, 5, 35

public class A {
	void foo(C c,C c2) {
		int x= c.getObj().getX();
		int v1 = x;
		c.getObj().setX(0);
		int x2= c.getObj().getX();
		int v2 = x2;
		c.getObj().setX(3);
		int x3= c.getObj().getX();
		int v3 = x3;
		c.setObj(c);
		int x4= c.getObj().getX();
		int v4 = x4;
		c = new C();
		int x5= c.getObj().getX();
		int v5 = x5;
		c2.setObj(c);
		int v6 = x5;
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