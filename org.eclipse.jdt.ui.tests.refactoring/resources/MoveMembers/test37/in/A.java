package p;
public class A {
	Inner i;
	A.Inner ii;
	p.A.Inner iii;
	static class Inner {
		Inner buddy;
		public Inner(A.Inner other) {
			buddy= other;
		}
	}
}

class AA {
	A.Inner Inner;
}
