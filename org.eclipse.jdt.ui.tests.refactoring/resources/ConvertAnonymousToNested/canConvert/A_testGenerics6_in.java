package p;
//private, static, final
class A<T>{
	A(){
	}
	<S> void f(){
		new A<S>(){
			T t;
		};
	}
}
