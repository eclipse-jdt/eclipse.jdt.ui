//3, 19 -> 3, 24   AllowLoadtime == false
package p;
@SuppressWarnings(A.S_ALL)
class A {
	static final String S_ALL= "all";
	@SuppressWarnings({S_ALL, "rawtypes"})
	int v= 0;
	void m() {
		@SuppressWarnings(value= S_ALL + "a")
		int v= 1;
	}
}
