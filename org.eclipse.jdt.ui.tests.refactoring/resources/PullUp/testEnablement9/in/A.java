package p;
class A<T>{
}
class Outer<T>{
	class B<T> extends A<T>{
		/**
	 	* comment
	 	*/
		void f(){}
	}
}