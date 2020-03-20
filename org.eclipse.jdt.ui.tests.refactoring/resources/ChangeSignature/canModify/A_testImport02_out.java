package p;

import java.util.Date;

abstract class A {
	public abstract int m(Date d, java.sql.Date sql);
	Date date;
	protected void finalize() {
		m(null, null);
	}
}

class B extends A {
	public int m(Date d, java.sql.Date sql) {
		return 17;
	}
}
