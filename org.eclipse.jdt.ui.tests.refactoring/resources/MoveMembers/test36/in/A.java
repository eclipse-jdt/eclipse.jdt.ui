package p;
public class A {
	public static class I {
		public I(I other) {
			a= new A();
		}
		public static class J {
			static int bla;
			int x() {return 1;}
		}
	}
	public static A a;
	public A.I i;
	{
		i= new A.I(i);
		int blub= I.J.bla + new I.J().x();
	}
}
