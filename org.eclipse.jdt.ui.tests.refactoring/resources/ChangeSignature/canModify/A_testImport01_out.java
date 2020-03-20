package p;

import java.util.Date;

abstract class A {
	public abstract int m(java.sql.Date sql, Date d);
	Date date;
	protected void finalize() {
		m(null, date);
	}
}

class B extends A {
	public int m(java.sql.Date sql, Date d) {
		return 17;
	}
}
