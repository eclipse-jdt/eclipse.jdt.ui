package p;
class A<? extends Object>{
}
class D<S> extends A<S>{
	void f(){}
}
class B extends A<String>{
	/**
	 * comment
	 */
	void f(){}
}