public class Foo {
	int j = 0;
	
	void toBeRefactored() {
		this.j = 1;
		new Other() {
			@Override
			void toImplement() {
				toCall();
			}
		};
	}
}
