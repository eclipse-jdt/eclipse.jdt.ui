package p;
import static java.util.Arrays.asList;
public class A {	
	class X {
		void m(B b) {
			asList(b.toString());
			n();
		}
	}
	
	public static void n() {
	}
}

class B {
	
}