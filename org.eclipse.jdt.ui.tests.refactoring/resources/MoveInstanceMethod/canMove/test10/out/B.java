package p2;

public class B {
	public void mB1() {}
	
	public void mB2() {}

	/**
	 * @param j
	 * @param foo
	 * @param bar
	 */
	public void mA1(float j, int foo, String bar) {
		mB1();
		System.out.println(bar + j);
	}
}