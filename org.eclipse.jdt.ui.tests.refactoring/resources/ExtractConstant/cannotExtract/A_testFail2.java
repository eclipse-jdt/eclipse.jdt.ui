//9, 20 -> 9, 21   AllowLoadtime == true
package p;

class R {
	int r;
	
	static class S extends R {
		void f(){
			int n= r;
		}
	}
}