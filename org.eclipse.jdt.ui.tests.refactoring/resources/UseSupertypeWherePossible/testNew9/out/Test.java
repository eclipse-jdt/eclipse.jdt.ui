package p;

class Test{
	void test(){
		Object a= new A();
		test(a);
	}
	void test(Object o){
		o.hashCode();
	}
}