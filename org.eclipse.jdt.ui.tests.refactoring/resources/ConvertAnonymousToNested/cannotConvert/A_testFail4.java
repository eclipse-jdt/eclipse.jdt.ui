package p;
class A {
	static void foo() {
		final int x= 10; 
		Runnable runnable= new Runnable() {
			private int field;
			public void run() {
				I i= new I() {
					public void method() {
						field= x;
					}
				};
			}
		};
	}
}

interface I {
	void method();
}