package p;

import java.security.Permission;

abstract class A {
	public abstract int m();
	Permission perm;
	protected void finalize() {
		m();
	}
}

class B extends A {
	public int m() {
		return 17;
	}
}
