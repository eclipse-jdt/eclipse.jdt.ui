package pkg1;

public class A implements PullUpToInterfaceBug.Foo {
	public void b() {
	}

	@Override
	public void baz4() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void baz3() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void baz2() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void baz1() {
		// TODO Auto-generated method stub
		
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