package p;
class A {
	void f(){
		new Comparable(){
			public int compareTo(Object other) {
				return 0;
			}
		};
	}
}