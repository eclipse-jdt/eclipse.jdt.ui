package p;
class A {
	interface I{
		void foo();
	}
	static I i = new I(){
		public void foo(){
			I i = new I() {
				public void foo(){
					
				}
			};
		}
	};
}
