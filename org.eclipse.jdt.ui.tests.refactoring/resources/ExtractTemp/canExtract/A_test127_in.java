package p; //6, 32, 6, 47

class A {
	void method(B b) {
		if (b instanceof D) {
			System.out.println(((C) b).getV2());
		}
		if (b instanceof C) {
			System.out.println(((C) b).getV2());
		}

	}
}


class B {
	int v1;

	public B(int v1) {
		super();
		this.v1 = v1;
	}

	public int getV1() {
		return v1;
	}
}

class C extends B {
	int v2;

	public C(int v2) {
		super(v2 * 10);
		this.v2 = v2;
	}

	public int getV2() {
		return v2;
	}
}

class D extends C {
	int v3;

	public D(int v3) {
		super(v3 * 10);
		this.v3 = v3;
	}

	public int getV3() {
		return v3;
	}
}