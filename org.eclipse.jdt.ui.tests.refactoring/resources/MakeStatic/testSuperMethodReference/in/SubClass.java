package p;

import java.util.function.BiFunction;

public class SubClass extends SuperClass {
	public SubClass() {
		BiFunction<String, String, String> function= (s1, s2) -> this.bar(s1, s2);
		function= SubClass.super::bar;
	}
}
