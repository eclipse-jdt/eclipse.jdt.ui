package p;

import static org.eclipse.jdt.annotation.DefaultLocation.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })
class TestNoRedundantNonNull2 {
	String string = "A";
	Integer integer = 2;
	Map<String, ? extends Number> map = new HashMap<>();
	Object[][] array = { {}, {} };

	public static class FooParameter {
		private String s1;
		private Integer h1;
		private Map<String, ? extends Number> map1;
		private Object[][] array1;
		public FooParameter(String s1, Integer h1, Map<String, ? extends Number> map1, Object[][] array1) {
			this.s1 = s1;
			this.h1 = h1;
			this.map1 = map1;
			this.array1 = array1;
		}
		public String getS1() {
			return s1;
		}
		public Integer getH1() {
			return h1;
		}
		public Map<String, ? extends Number> getMap1() {
			return map1;
		}
		public Object[][] getArray1() {
			return array1;
		}
	}

	void foo(FooParameter parameterObject) {
		string = parameterObject.getS1();
		integer = parameterObject.getH1();
		map = parameterObject.getMap1();
		array = parameterObject.getArray1();
	}
}