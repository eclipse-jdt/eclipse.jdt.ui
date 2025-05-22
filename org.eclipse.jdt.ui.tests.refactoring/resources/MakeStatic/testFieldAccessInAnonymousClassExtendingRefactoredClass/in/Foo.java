public class Foo {
    private final int counter;

    public Foo(int initialCounter) {
        counter = initialCounter;
    }

    void toBeRefactored() { // make static refactoring on this method
        new Foo(counter + 10) {
            void toImplement() {
            	System.out.println(counter);
                // Calling outer class method directly
                Foo.this.toCall();
            }
        }.toImplement();
    }

    void toCall() {
        System.out.println("Counter: " + counter);
    }

    public static void main(String[] args) {
        Foo foo = new Foo(5);
        foo.toBeRefactored();
    }
}
