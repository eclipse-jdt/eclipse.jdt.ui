package p;

import java.security.acl.Permission;

abstract class A {
	public abstract int m(Permission p, java.security.Permission pp);
	protected void finalize() {
		m(0, 0);
	}
}

class B extends A {
	public int m(Permission p, java.security.Permission pp) {
		return 17;
	}
}
