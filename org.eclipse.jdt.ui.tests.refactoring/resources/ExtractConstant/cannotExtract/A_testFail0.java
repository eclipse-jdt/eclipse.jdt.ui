//8, 16 -> 8, 21   AllowLoadtime == true
package p;

static class S {
	int s;
	
	void f(){
		int l= s + 1;
	}
}