public class A_test4TypeParameters_in {
	void foo(){
		A<String,Integer,Float,Double> a = null;
	}
}
interface I<T1,T2>{ }
interface J<T3,T4> extends I<T4,T3>{ }
class A<T5,T6,T7,T8> implements I<T5,T6>, J<T7,T8> { }
