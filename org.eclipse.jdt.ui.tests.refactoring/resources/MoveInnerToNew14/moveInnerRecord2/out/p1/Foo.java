package p1;

public record Foo(Bar bar) {
    public String foo(Bar<String> in) {
    	return in.left();
    }
}