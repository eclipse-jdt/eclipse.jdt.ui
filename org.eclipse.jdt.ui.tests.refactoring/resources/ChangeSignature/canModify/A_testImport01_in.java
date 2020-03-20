package p;

import java.util.Date;

abstract class A {
	public abstract int m();
	Date date;
	protected void finalize() {
		m();
	}
}

class B extends A {
	public int m() {
		return 17;
	}
}
