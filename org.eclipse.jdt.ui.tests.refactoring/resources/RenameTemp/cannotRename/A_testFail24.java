//compile error
package p;
class A{
	void m(int f){
		final int /*[*/i/*]*/= 0;
		m(t);
	};
}