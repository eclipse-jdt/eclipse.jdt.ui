package p;
public class B{

	public static class I {
		public I(I other) {
			A.a= new A();
		}
		public static class J {
			static int bla;
			int x() {return 1;}
		}
	}
}
