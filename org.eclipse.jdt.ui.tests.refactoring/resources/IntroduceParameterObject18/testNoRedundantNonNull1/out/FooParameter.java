package p;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

public class FooParameter {
	private @NonNull String s1;
	private @NonNull Integer h1;
	private @NonNull Map<@NonNull String, ? extends @NonNull Number> map1;
	private @NonNull Object @NonNull [] @NonNull [] array1;
	public FooParameter(@NonNull String s1, @NonNull Integer h1, @NonNull Map<@NonNull String, ? extends @NonNull Number> map1, @NonNull Object @NonNull [] @NonNull [] array1) {
		this.s1 = s1;
		this.h1 = h1;
		this.map1 = map1;
		this.array1 = array1;
	}
	public @NonNull String getS1() {
		return s1;
	}
	public @NonNull Integer getH1() {
		return h1;
	}
	public @NonNull Map<@NonNull String, ? extends @NonNull Number> getMap1() {
		return map1;
	}
	public @NonNull Object @NonNull [] @NonNull [] getArray1() {
		return array1;
	}
}