package p;
class A extends Outer.B<String>{
}
class Outer<S>{
	class B<S>{
		/**
	 	* comment
	 	*/
		void f(){}
	}
}