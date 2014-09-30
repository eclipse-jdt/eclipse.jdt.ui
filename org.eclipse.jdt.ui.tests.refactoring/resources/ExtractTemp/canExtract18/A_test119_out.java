package p; //7, 30, 7, 63

import java.util.function.Supplier;

public class A {
	{
		Supplier<String> supplier= () -> (new Integer(0)).toString();
		Supplier<String> a2= supplier;
	}
}
