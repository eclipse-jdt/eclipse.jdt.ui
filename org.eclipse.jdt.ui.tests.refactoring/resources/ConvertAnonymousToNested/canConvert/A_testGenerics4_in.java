package p;
//private, static, final
class A<T>{
	A(){
	}
	void f(){
		new A<T>(){};
	}
}
