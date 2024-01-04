public class Foo {
	public void method() {
		new Runnable() {
			private void bar() {
			}

			@Override
			public void run() {
				Runnable r= this::bar;
			}
		};
	}
}
