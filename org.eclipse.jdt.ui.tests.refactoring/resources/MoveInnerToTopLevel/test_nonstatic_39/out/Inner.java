package p;

import p.A.Stat;

class Inner {
	public void doit() {
		A.foo();
		A.fred++;
		new Stat();
	}
}