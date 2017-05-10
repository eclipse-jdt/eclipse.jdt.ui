package p;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

public class NoRedundantNonNull1Data {
	private @NonNull String string;
	private @NonNull Integer integer;
	private @NonNull Map<@NonNull String, ? extends @NonNull Number> map;
	private @NonNull Object @NonNull [] @NonNull [] array;
	public NoRedundantNonNull1Data(@NonNull String string, @NonNull Integer integer, @NonNull Map<@NonNull String, ? extends @NonNull Number> map, @NonNull Object @NonNull [] @NonNull [] array) {
		this.string = string;
		this.integer = integer;
		this.map = map;
		this.array = array;
	}
	public @NonNull String getString() {
		return string;
	}
	public void setString(@NonNull String string) {
		this.string = string;
	}
	public @NonNull Integer getInteger() {
		return integer;
	}
	public void setInteger(@NonNull Integer integer) {
		this.integer = integer;
	}
	public @NonNull Map<@NonNull String, ? extends @NonNull Number> getMap() {
		return map;
	}
	public void setMap(@NonNull Map<@NonNull String, ? extends @NonNull Number> map) {
		this.map = map;
	}
	public @NonNull Object @NonNull [] @NonNull [] getArray() {
		return array;
	}
	public void setArray(@NonNull Object @NonNull [] @NonNull [] array) {
		this.array = array;
	}
}