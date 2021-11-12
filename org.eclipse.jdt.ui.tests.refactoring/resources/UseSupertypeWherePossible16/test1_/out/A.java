package p;
//change to B
class B {
	void m();
} 
class A extends B{
	void f(Object x){
		B a= null;
		B a1= a;
		B a2= a1;
		a1.m();
		A a3= null;
		a3.f(null);
		if (x instanceof B) {
			((B) x).m();
		}
		if (x instanceof A z) {
			B a4= null;
			z.m();
		}
	}
}