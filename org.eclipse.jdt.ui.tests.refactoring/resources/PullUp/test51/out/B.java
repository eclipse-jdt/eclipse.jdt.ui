package pkg1;

import java.util.List;

public class B {

	interface Foo {

		void b();

	}

	static class Bar implements Foo {

		/** baz it! */
		void baz(final String s) {
		}

		/* (non-Javadoc)
		 * @see p.B.Foo#b()
		 */
		@Override
		public void b() {
			// TODO Auto-generated method stub
			
		}
	}
}
