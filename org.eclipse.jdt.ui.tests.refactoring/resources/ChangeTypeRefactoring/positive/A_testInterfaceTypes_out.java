
public class A_testInterfaceTypes_in {
	void foo(){
		I b = new C();
		b.toString();
	}
}

interface I { }
class A implements I { }
class B extends A implements I { }
class C extends B implements I { }
