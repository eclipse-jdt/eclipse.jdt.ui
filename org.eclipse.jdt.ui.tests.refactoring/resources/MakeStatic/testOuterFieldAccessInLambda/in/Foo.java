public class Foo {
	public void bar() {
		Runnable runnable= () -> {
			System.out.println(Foo.this);
		};
		new Thread(runnable).start();
	}
}
