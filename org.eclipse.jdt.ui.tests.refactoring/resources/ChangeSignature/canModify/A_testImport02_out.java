package p;

import java.security.Permission;

abstract class A {
	public abstract int m(Permission p, java.security.acl.Permission acl);
	Permission perm;
	protected void finalize() {
		m(null, null);
	}
}

class B extends A {
	public int m(Permission p, java.security.acl.Permission acl) {
		return 17;
	}
}
