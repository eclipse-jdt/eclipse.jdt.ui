package p;
class Test{
	void test(){
		Object a= new A();
		f(a);
	}
	void f(Object a){
		a.getClass();
	}
}