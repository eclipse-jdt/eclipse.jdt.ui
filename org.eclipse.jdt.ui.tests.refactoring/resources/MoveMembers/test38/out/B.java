package r;

public class B {

	public static class Inner {
		Inner buddy;
		public Inner(Inner other) {
			buddy= other;
		}
	}
}
