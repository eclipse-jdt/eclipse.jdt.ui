public class Foo {
	
	void toBeRefactored() {
		new Foo() {
			void toImplement() {
				toCall();
			}
		};
	}
	
	void toCall() { }
	
	void toImplement() { }
	
}
