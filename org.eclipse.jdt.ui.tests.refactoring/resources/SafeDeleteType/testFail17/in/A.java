package p;
class A{
static int x(){};
}
class B{
	void f(){
		Object f= (A)new A();
	}
}