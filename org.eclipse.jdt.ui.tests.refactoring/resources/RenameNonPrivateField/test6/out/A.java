package p;
class A{
	protected int g;
	void m(){
		g++;
	}
}

class AA extends A{
	protected int f;
}

class B{
	A a;
	B b;
	A ab= new B();
	void m(){
		a.g= 0;
		b.f= 0;
		ab.g= 0;
	}
}