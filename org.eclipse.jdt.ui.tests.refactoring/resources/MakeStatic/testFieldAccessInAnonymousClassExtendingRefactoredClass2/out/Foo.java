public class Foo {
    public final int counter;

    public Foo(int initialCounter) {
        counter = initialCounter;
    }

    static void toBeRefactored(Foo foo) { // make static refactoring on this method
        new Foo(foo.counter + 10) {
            void toImplement() {
            	System.out.println(counter);
                // Calling outer class method directly
                foo.toCall();
            }
        }.toImplement();
    }

    void toCall() {
        System.out.println("Counter: " + counter);
    }

    public static void main(String[] args) {
        Foo foo = new Foo(5);
        Foo.toBeRefactored(foo);
    }
}
