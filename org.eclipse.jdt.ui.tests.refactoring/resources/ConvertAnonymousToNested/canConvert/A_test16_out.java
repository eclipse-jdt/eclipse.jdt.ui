package p;
class A {
	private static final class Inner implements Comparable {
		public int compareTo(Object other) {
		    return 0;
		}
	}
    void f(){
        new Inner();
    }
}