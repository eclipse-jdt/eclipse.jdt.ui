import java.util.Vector;

public class A_testReorderTypeParameters_in {
	void foo(){
		Vector v = new Vector();
		A<Integer, String> x = new B<String, Integer>();
	}
}
class A<T1,T2> { }
class B<T3,T4> extends A<T4,T3> { }
