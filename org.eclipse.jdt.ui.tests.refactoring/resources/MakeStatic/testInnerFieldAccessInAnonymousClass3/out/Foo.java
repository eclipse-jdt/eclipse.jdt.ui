public class Foo {

	String instance;

	static void bar(Foo foo, int i) {

		foo.instance= "";

		new Thread() {
			void innerMethod() {
			};

			public void foo() {
				this.innerMethod();
			}
		};
	}
}
