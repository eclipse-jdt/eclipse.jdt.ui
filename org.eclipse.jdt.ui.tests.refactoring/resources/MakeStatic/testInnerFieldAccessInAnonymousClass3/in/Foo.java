public class Foo {

	String instance;

	void bar(int i) {

		this.instance= "";

		new Thread() {
			void innerMethod() {
			};

			public void foo() {
				this.innerMethod();
			}
		};
	}
}
