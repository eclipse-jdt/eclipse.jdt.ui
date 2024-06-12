package p1;

public record Foo(Foo.Bar bar) {
    static record Bar<T> (T left, T right) {}
    
    public String foo(Bar<String> in) {
    	return in.left();
    }
}