package p;

public class TwoTypeParams_in {
	public void foo() {
		Pair<String,String> p1= Pair.createPair("", "");
		Pair<Integer,String> p2= Pair.createPair(3, "");
		Pair<Float,Object> p3= Pair.createPair(3.14F, null);
	}
}
class Pair<T1,T2> {
	public static <T1, T2> Pair<T1, T2> createPair(T1 t1, T2 t2) {
		return new Pair<T1, T2>(t1, t2);
	}
	T1 fLeft;
	T2 fRight;
	private /*[*/Pair/*]*/(T1 t1, T2 t2) {
		fLeft= t1;
		fRight= t2;
	}
}
