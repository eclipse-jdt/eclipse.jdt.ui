package pkg1;

public class A implements PullUpToInterfaceBug.Foo {
	public void b() {
	}
}

class PullUpToInterfaceBug {

	interface Foo {
	}

	static class B implements Foo {
		public void baz1() {
		}
		protected void baz2() {
		}
		void baz3() {
		}
		private void baz4() {
		}
	}
}