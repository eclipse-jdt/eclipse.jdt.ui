package p;
//public, nonstatic, final
class A<T>{
	void f(){
		new A<T>(){};
	}
}
