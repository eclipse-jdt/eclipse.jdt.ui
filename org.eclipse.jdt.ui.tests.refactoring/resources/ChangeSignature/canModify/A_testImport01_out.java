package p;

import java.security.Permission;

abstract class A {
	public abstract int m(java.security.acl.Permission acl, Permission p);
	Permission perm;
	protected void finalize() {
		m(null, perm);
	}
}

class B extends A {
	public int m(java.security.acl.Permission acl, Permission p) {
		return 17;
	}
}
