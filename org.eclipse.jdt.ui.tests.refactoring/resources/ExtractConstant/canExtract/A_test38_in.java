//3, 19 -> 3, 24   AllowLoadtime == false
package p;
@SuppressWarnings("all")
class A {
	@SuppressWarnings({"all", "rawtypes"})
	int v= 0;
	void m() {
		@SuppressWarnings(value= "all" + "a")
		int v= 1;
	}
}
