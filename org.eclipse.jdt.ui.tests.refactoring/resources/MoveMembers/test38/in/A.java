package p;

public class A {
	Inner i;
	A.Inner ii;
	p.A.Inner iii;
	public static class Inner {
		Inner buddy;
		public Inner(Inner other) {
			buddy= other;
		}
	}
}
