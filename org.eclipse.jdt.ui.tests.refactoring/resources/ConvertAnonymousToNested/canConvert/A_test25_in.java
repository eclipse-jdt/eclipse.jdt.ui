package p;
class A {
	interface I{
		void foo();
	}
	static A i = new A(){
		public void foo(){
			I i = new I() {
				public void foo(){
					
				}
			};
		}
	};
}
