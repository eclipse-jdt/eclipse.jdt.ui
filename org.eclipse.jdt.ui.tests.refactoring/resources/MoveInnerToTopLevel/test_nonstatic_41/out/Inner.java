package p;

import static java.lang.Math.E;
import p.A.Stat;

class Inner {
	static class InnerInner {
		static class InnerInnerInner {}
	}
	public void doit() {
		A.foo();
		A.fred++;
		double e= E;
		new Stat();
	}
}