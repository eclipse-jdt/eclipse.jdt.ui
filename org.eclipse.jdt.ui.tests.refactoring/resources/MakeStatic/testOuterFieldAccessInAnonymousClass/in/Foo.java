public class Foo {
	public void bar() {
		new Thread() {
			public void run() {
				System.out.println(Foo.this);
			}
		}.start();
	}
}
