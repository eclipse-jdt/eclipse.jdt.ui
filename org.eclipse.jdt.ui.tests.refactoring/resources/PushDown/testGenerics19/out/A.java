package p;
abstract class A<T>{

	public abstract void f();

	public abstract T m(T t);
}
abstract class B extends A<String>{

	@Override
	public String m(String t) {
		String s= t;
		return null;
	}

	public abstract void f();
}
class C extends A<Object>{
	public void f(){}

	@Override
	public Object m(Object t) {
		Object s= t;
		return null;
	}
}