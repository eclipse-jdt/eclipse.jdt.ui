package p;

public class CtorThrows_in {
	private int	fValue;

	public CtorThrows_in(int x) throws IllegalArgumentException {
		if (x < 0) throw IllegalArgumentException("Bad value: " + x);
		fValue= x;
	}

	public static void main(String[] args) {
		CtorThrows_in cti= /*[*/new CtorThrows_in(3)/*]*/;
	}
}
