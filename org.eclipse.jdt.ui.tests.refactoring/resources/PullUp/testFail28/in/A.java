package p;

class Super{
	static class A {
	}
}
class B extends Super.A{
	public static class Test {
		static class X{
			class Super{}
		}
	}
}