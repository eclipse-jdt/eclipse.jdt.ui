package p;
abstract class A<T>{
}
abstract class B extends A<String>{

	public abstract void m();
}
abstract class B1 extends B{
}
abstract class C extends A<String>{

	public abstract void m();
}