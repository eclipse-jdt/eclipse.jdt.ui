package p;

public class ProtectConstructor2_in {
	private int		fX, fY;

	public /*[*/ProtectConstructor2_in/*]*/(int x, int y) {
		fX= x;
		fY= y;
	}
	public int getX() {
		return fX;
	}
	public int getY() {
		return fY;
	}
	public static void main(String[] args) {
		int						y=  20;
		ProtectConstructor2_in	pc= new ProtectConstructor2_in(15, y);

		System.out.println("Value = " + Integer.toHexString(pc.getX() + pc.getY()));
	}
}
