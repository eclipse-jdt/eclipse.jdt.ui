package staticMethods_in;

interface A_test103 {
	int i= 0;
	int j= /*[*/extracted();/*]*/
	static int extracted() {
		return i + 10;
	}
}