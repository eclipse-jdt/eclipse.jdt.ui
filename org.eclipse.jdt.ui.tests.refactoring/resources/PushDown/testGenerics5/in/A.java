package p;
abstract class A<T>{
	public abstract void m();
}
abstract class B extends A<String>{
}
abstract class B1 extends B{
}
abstract class C extends A<String>{
}