package p;
class A {
	static class Inner{
		void foo() {
			Inner2.Inner2Inner a;
		}
	}
	static class Inner2{
		static class Inner2Inner{
		}
	}
}