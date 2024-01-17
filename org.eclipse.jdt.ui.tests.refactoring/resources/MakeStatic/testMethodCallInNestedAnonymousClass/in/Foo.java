public class Foo {
	int j = 0;
	
	void toBeRefactored() {
		this.j = 1;
		new Other() {
			@Override
			void toImplement() {
				new Object() {
					@Override
					public boolean equals(Object obj) {
						toCall();
						return false;
					}
				};
			}
		};
	}
}
