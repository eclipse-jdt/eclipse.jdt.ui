package p;

import p.A.Stat;

class Inner {
	static class InnerInner {
		static class InnerInnerInner {}
	}
	public void doit() {
		A.foo();
		A.fred++;
		new Stat();
	}
}