public class Foo {
	int j = 0;
	
	static void toBeRefactored(Foo foo) {
		foo.j = 1;
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
