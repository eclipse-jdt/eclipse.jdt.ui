//6, 16 -> 6, 20   AllowLoadtime == true
package p;

class S {
	int f(){
		return f2();	
	}
	int f2() {
		return 0;
	}
}	