//cannot rename to: j
package p;

class B{
	int j;
	class A {
		int m(){
			int /*[*/i/*]*/=0;
			i= j;
			return 0;
		};
	}
}