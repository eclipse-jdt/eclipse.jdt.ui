//cannot rename to j
package p;
class A{
	void m(){
		final int /*[*/i/*]*/= 0;
		A a= new A(){
			void m(int j){
				int u= i;
			}
		};
	}
}