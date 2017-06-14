package p;

import static org.eclipse.jdt.annotation.DefaultLocation.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })
class NoRedundantNonNull2 {
	public static class NoRedundantNonNull2Data {
		private String string;
		private Integer integer;
		private Map<String, ? extends Number> map;
		private Object[][] array;
		public NoRedundantNonNull2Data(String string, Integer integer, Map<String, ? extends Number> map, Object[][] array) {
			this.string = string;
			this.integer = integer;
			this.map = map;
			this.array = array;
		}
		public String getString() {
			return string;
		}
		public void setString(String string) {
			this.string = string;
		}
		public Integer getInteger() {
			return integer;
		}
		public void setInteger(Integer integer) {
			this.integer = integer;
		}
		public Map<String, ? extends Number> getMap() {
			return map;
		}
		public void setMap(Map<String, ? extends Number> map) {
			this.map = map;
		}
		public Object[][] getArray() {
			return array;
		}
		public void setArray(Object[][] array) {
			this.array = array;
		}
	}

	NoRedundantNonNull2Data data = new NoRedundantNonNull2Data("A", 2, new HashMap<>(), new @NonNull Object @NonNull [] @NonNull []{ {}, {} });

	NoRedundantNonNull2(String s1, Integer h1, Map<String, ? extends Number> map1, Object[][] array1) {
		data.setString(s1);
		data.setInteger(h1);
		data.setMap(map1);
		data.setArray(array1);
	}
}