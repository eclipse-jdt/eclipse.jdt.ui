package p;
abstract class A<T>{
	public abstract T m();
}
class B extends A<String>{

	public String m() {return null;}
}
class B1 extends B{
}
class C extends A<String>{

	public String m() {return null;}
}