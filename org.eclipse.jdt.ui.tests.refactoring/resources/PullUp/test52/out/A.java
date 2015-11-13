package pkg1;

public class A implements PullUpToInterfaceBug.Foo {
	public void b() {
	}

	/* (non-Javadoc)
	 * @see p.PullUpToInterfaceBug.Foo#baz4()
	 */
	public void baz4() {
	}

	/* (non-Javadoc)
	 * @see p.PullUpToInterfaceBug.Foo#baz3()
	 */
	public void baz3() {
	}

	/* (non-Javadoc)
	 * @see p.PullUpToInterfaceBug.Foo#baz2()
	 */
	public void baz2() {
	}

	/* (non-Javadoc)
	 * @see p.PullUpToInterfaceBug.Foo#baz1()
	 */
	public void baz1() {
	}
}

class PullUpToInterfaceBug {

	interface Foo {

		void baz4();

		void baz3();

		void baz2();

		void baz1();
	}

	static class B implements Foo {
		@Override
		public void baz1() {
		}
		@Override
		public void baz2() {
		}
		@Override
		public void baz3() {
		}
		@Override
		public void baz4() {
		}
	}
}