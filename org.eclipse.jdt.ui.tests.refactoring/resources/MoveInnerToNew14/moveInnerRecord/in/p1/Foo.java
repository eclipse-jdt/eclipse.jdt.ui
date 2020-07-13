package p1;

public record Foo(Foo.Bar bar) {
    public record Bar(int i) {}
}