package p;
abstract class A<T>{
}
abstract class B extends A<String>{

	public void m(String t) {}

	public abstract void f();
}
class C extends A<Object>{
	public void f(){}

	public void m(Object t) {}
}