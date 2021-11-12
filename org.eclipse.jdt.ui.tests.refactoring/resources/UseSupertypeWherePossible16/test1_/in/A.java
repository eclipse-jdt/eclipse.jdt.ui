package p;
//change to B
class B {
	void m();
} 
class A extends B{
	void f(Object x){
		A a= null;
		A a1= a;
		A a2= a1;
		a1.m();
		A a3= null;
		a3.f(null);
		if (x instanceof A) {
			((A) x).m();
		}
		if (x instanceof A z) {
			A a4= null;
			z.m();
		}
	}
}