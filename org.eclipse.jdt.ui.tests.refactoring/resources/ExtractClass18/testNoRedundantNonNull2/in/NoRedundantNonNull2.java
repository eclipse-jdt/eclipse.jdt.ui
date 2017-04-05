package p;

import static org.eclipse.jdt.annotation.DefaultLocation.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })
class NoRedundantNonNull2 {
	String string = "A";
	Integer integer = 2;
	Map<String, ? extends Number> map = new HashMap<>();
	Object[][] array = { {}, {} };

	NoRedundantNonNull2(String s1, Integer h1, Map<String, ? extends Number> map1, Object[][] array1) {
		string = s1;
		integer = h1;
		map = map1;
		array = array1;
	}
}