package generic_in;

import java.util.ArrayList;

public class TestParameterizedType3 {
	class Inner<T extends ArrayList> {
		public final T[] get() {
			return (T[])new ArrayList[0];
		}
	}
	
	void use() {
		new Inner<ArrayList>()./*]*/get()/*[*/;
	}
}
