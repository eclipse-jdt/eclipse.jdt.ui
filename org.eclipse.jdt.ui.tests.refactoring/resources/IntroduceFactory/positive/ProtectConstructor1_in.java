package p;

public class ProtectConstructor1_in {
	private int		fX;

	public /*[*/ProtectConstructor1_in/*]*/(int x) {
		fX= x;
	}
	public int getX() {
		return fX;
	}
	public static void main(String[] args) {
		ProtectConstructor1_in	pc= new ProtectConstructor1_in(15);

		System.out.println("Value = " + Integer.toHexString(pc.getX()));
	}
}
