package p; //6, 59, 6, 59

import java.util.function.Supplier;

public class A {
	Supplier<String> a2= () -> {
		String string= (new Integer(0)).toString();
		return string;
	};
}
