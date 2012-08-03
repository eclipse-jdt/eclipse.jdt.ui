package p;

class A {
	public int i = 0;
}

class B extends A {
}

class C {

	public void m(B b) {
		b.i++;
		b.i++;
		b.i = b.i + 1;
		b.i = b.i + 1;
	}
}