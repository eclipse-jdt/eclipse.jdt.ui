public class X {
	class Inner extends Exception {
	}
}
class DD extends X.Inner {
	DD() {
		new X().super();
	}
	public final static boolean DEBUG= true;
	public void foo0() {
		try {
			d();
		} catch (X.Inner e) {
		}
	}

	protected void d() throws X.Inner {
		if (DEBUG)
			throw new X().new Inner();//<<SELECT AND EXTRACT
	}

}