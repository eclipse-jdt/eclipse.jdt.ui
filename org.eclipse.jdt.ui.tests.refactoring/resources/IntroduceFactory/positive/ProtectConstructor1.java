package p;

public class ProtectConstructor1_in {
	private int		fX;

	private /*[*/ProtectConstructor1_in/*]*/(int x) {
		fX= x;
	}
	public int getX() {
		return fX;
	}
	public static void main(String[] args) {
		ProtectConstructor1_in	pc= createProtectConstructor1_in(15);

		System.out.println("Value = " + Integer.toHexString(pc.getX()));
	}
	public static ProtectConstructor1_in createProtectConstructor1_in(int x) {
		return new ProtectConstructor1_in(x);
	}
}
