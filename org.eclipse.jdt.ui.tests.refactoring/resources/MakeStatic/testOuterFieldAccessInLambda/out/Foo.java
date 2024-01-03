public class Foo {
	public static void bar(Foo foo) {
		Runnable runnable= () -> {
			System.out.println(foo);
		};
		new Thread(runnable).start();
	}
}
