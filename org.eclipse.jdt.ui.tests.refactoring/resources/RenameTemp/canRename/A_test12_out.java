//rename to j
package p;
class A{
	void m(){
		final int /*[*/j/*]*/= 0;
		A a= new A(){
			void m(int k){
				k= j;
			}
		};
	}
}