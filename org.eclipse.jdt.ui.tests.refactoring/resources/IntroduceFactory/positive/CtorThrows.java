package p;

public class CtorThrows_in {
	private int	fValue;

	private CtorThrows_in(int x) throws IllegalArgumentException {
		if (x < 0) throw IllegalArgumentException("Bad value: " + x);
		fValue= x;
	}

	public static void main(String[] args) {
		CtorThrows_in cti= createCtorThrows_in(3);
	}

	public static CtorThrows_in createCtorThrows_in(int x) throws IllegalArgumentException {
		return new CtorThrows_in(x);
	}
}
