package p;
class A{
static int x(){};
}
class B{
	class C{
		void f(){
		new A();
		}
	}
}