public class A_testUnrelatedTypeParameters_in {
	void foo(){
		E<String> e = new G<Integer>();
	}
}
class E<T>{ }
class F extends E<String>{ }
class G<U> extends F { }
