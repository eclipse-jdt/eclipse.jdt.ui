package p;

public class DontMoveArgs1_in {
	private int		fN;

	public /*[*/DontMoveArgs1_in/*]*/(int N) {
		fN= N;
	}
	public int getN() {
		return fN;
	}
	public static void main(String[] args) {
		DontMoveArgs1_in	dma= new DontMoveArgs1_in(15);

		System.out.println("Value = " + Integer.toHexString(dma.getN()));
	}
}
