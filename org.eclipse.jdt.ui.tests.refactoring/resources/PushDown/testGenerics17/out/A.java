package p;
abstract class A<T>{

	public abstract T f();
}
abstract class B<S> extends A<String>{

	public abstract String f();
}
class C extends A<Object>{
	public Object f(){return null;}
}