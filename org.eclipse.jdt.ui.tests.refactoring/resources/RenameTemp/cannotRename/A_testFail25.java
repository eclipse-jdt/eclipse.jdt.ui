//cannot rename i to j
package p;
class A{
	void m(){
		final int /*[*/i/*]*/= 0;
		new A(){
			void f(){
				int j= 0;
				int i2= i;
			}
		};
	};
}