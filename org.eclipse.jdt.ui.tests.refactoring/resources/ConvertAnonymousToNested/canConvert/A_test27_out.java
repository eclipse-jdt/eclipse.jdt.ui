package p;
class A {
	private final class Inner implements I {
		public void foo(){
		}
	}
	interface I{
		void foo();
	}
	static void foo1(){
		new A(){
			void foo(){
				I i = new I(){
					public void foo(){
						I i = new Inner();
					}
				};
			}
		};
	}
}
