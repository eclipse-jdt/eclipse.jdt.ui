public class Foo {
	int j = 0;
	
	static void toBeRefactored(Foo foo) {
		foo.j = 1;
		new Other() {
			@Override
			void toImplement() {
				toCall();
			}
		};
	}
}
