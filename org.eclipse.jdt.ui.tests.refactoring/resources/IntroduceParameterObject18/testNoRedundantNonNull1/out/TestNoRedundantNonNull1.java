package p;

import static org.eclipse.jdt.annotation.DefaultLocation.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })
class TestNoRedundantNonNull1 {
	String string = "A";
	Integer integer = 2;
	Map<String, ? extends Number> map = new HashMap<>();
	Object[][] array = { {}, {} };

	void foo(FooParameter parameterObject) {
		string = parameterObject.getS1();
		integer = parameterObject.getH1();
		map = parameterObject.getMap1();
		array = parameterObject.getArray1();
	}
}