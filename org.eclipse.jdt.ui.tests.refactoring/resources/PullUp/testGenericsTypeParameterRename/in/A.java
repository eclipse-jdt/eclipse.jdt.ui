package p;

public class A {
    public interface Bar<T> { }

    public interface Base<T> { }

    public class B<T,U> implements Base<U> {
    	public void m(Bar<U> bar) { }// pull method foo up to interface Base
    }
}