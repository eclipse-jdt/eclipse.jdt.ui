package p;
class A {
	interface I{
		void foo();
	}
	static void foo1(){
		new A(){
			void foo(){
				I i = new I(){
					public void foo(){
						I i = new I(){
							public void foo(){
							}
						};
					}
				};
			}
		};
	}
}
