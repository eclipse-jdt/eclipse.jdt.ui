package p;

class A<T extends A<T>> {}

class B<T> extends A<B<T>> {}

class C extends B<C> {
	
	void cOnly() {
		B<C> x= new C();
		C y= new C();
		y.cOnly();
		B<C> z= new C();
	}
}
