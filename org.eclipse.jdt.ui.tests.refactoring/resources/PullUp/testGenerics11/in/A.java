package p;

class A<X> {
	int x;
}

class B<X> extends A<X> {
	protected void m() { 
		class T<X> extends A<X>{
			void t(){
				super.x++;
			}
		};
	}
}
