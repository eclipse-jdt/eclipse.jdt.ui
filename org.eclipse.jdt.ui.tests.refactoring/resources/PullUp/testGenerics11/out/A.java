package p;

class A<X> {
	int x;

	protected void m() { 
		class T<X> extends A<X>{
			void t(){
				super.x++;
			}
		};
	}
}

class B<X> extends A<X> {
}
