package p;
abstract class A<T>{

	public abstract void f();

	public T m(T t) {
		T s= t;
		return null;
	}
}
abstract class B extends A<String>{
}
class C extends A<Object>{
	public void f(){}
}