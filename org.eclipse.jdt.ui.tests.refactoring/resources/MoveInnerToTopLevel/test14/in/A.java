package p;
class A {
	static class Inner{
		static void foo() {
		}
		static int t= 1;
	}
	static void f(){
		Inner i;
		A.Inner i2;
		Inner.foo();
		Inner.t =  2;
		A.Inner.foo();
		A.Inner.t =  2;
		p.A.Inner.foo();
		p.A.Inner.t =  2;
	}
}