package p;

public class B {
	private int count;

	public void f() {
		Inner s= new Inner();
		print(s);
	}

	/**
	 * Bla bla
	 * @param s
	 */
	public void print(Inner s) {
		s.print(this);
	}	

	public class Inner {

		/**
		 * Bla bla
		 * @param b TODO
		 */
		public void print(B b) {
			b.count++;
			System.out.println(this);
		}
	}
}
