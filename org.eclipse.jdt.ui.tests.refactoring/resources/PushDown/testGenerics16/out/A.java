package p;
abstract class A<T>{
}
abstract class B<T> extends A<String>{

	public abstract String f();
}
class C extends A<Object>{
	public Object f(){return null;}
}