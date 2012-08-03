package p;

class A {
	public int i = 0;
}

class B extends A {
	public void m(C target) {
		i++;
		this.i++;
		i = i + 1;
		this.i = this.i + 1;
	}
}

class C {
}