package p;
//public, nonstatic, nonfinal
class A<T>{
	void f(){
		new A<T>(){};
	}
}
