package p;

class Tuple<T1, T2> {
	private T1 t1;
	private T2 t2;
	
	public Tuple(T1 o1, T2 o2) {
		t1= o1;
		t2= o2;
	}
}

class Main {
	public static void main(String[] args) {
		Tuple<String, Integer> t= new Tuple<String, Integer>(new String(), new Integer(10));
	}
}
