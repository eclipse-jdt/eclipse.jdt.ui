package p;
public class B {
	static class Inner {
		Inner buddy;
		public Inner(B.Inner other) {
			buddy= other;
		}
	}

	B.Inner iFromB;
}
