package p;

import java.io.IOException;

abstract class A {
	public abstract int m(long l) throws IOException;
}

class B extends A {
	public int m(long l) throws IOException {
		return 17;
	}
	public int m(int i) {
		return i;
	}
}

class C extends B {
}

class D extends A {
	public int m(long l) {
		return 0;
	}
}	