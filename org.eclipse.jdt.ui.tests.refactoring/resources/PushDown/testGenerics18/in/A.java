package p;
abstract class A<T>{

	public abstract void f();

	public void m(T t) {}
}
abstract class B extends A<String>{
}
class C extends A<Object>{
	public void f(){}
}