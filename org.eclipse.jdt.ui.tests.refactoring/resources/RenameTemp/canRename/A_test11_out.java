//rename to j
package p;
class A{
	int k;
	void m(){
		int /*[*/j/*]*/= 0;
		A a= new A(){
			void m(int i){
				i++;
			}
		};
	}
}