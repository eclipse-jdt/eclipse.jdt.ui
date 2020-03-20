package p;

import java.sql.Date;

abstract class A {
	public abstract int m(Date d, java.util.Date dd);
	protected void finalize() {
		m(0, 0);
	}
}

class B extends A {
	public int m(Date d, java.util.Date dd) {
		return 17;
	}
}
