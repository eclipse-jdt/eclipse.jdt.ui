package p;
abstract class A<T>{

	public abstract T f();
}
abstract class B<T> extends A<String>{
}
class C extends A<Object>{
	public Object f(){return null;}
}