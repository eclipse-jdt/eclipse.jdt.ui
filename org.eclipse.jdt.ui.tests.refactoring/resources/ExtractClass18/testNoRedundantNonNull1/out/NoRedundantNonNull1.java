package p;

import static org.eclipse.jdt.annotation.DefaultLocation.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault({ PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS })
class NoRedundantNonNull1 {
	NoRedundantNonNull1Data data = new NoRedundantNonNull1Data("A", 2, new HashMap<>(), new @NonNull Object @NonNull [] @NonNull []{ {}, {} });

	NoRedundantNonNull1(String s1, Integer h1, Map<String, ? extends Number> map1, Object[][] array1) {
		data.setString(s1);
		data.setInteger(h1);
		data.setMap(map1);
		data.setArray(array1);
	}
}