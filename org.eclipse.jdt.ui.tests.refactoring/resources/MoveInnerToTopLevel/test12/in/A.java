package p;
class A {
	static class Inner{
		void foo() {
			Inner2 a;
		}
	}
	static class Inner2{
		static class Inner2Inner{
		}
	}
}