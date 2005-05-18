package generic_out;

import java.util.ArrayList;

public class TestParameterizedType3 {
	class Inner<T extends ArrayList> {
		public final T[] get() {
			return (T[])new ArrayList[0];
		}
	}
	
	void use() {
		Inner<ArrayList> r = new Inner<ArrayList>();
		ArrayList[] get = (ArrayList[])new ArrayList[0];
	}
}
