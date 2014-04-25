package cast_in;

class Base {}
class Derived extends Base {}
interface I {
	public void foo(Derived d);
}
interface J extends Cloneable, I {
}
abstract class Goo implements Cloneable, J {
	public void foo(Base b) {
	    System.out.println("base");
	}
}
class Woo extends Goo {
	public void foo(Derived d) {
	    System.out.println("derived");
	}
}
public class TestHierarchyOverloadedMultiLevel {
	Base inlineMe() {
		return new Derived();
	}
	void main() {
		Goo goo = new Woo();
		goo.foo(/*]*/inlineMe()/*[*/);
	}
	public static void main(String[] args) {
	    new TestHierarchyOverloadedMultiLevel().main();
	}
}
