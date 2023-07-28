public class Foo {
	public static void bar(Foo foo) {
		new Thread() {
			public void run() {
				System.out.println(foo);
			}
		}.start();
	}
}
