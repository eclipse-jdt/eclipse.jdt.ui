package p;
//private, static, final
class A<T>{
	A(){
	}
	static <S> void f(){
		new A<S>(){};
	}
}
