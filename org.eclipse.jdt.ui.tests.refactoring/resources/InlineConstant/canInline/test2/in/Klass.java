// 10, 22 -> 10, 30  replaceAll == false

package p;

class Klass {
	static final Klass KONSTANT=           new   Klass()  ;
	
	
	static void f() {
		Klass klass= KONSTANT;	
	}
	
	Klass klass=KONSTANT;
}