//cannot rename to: j
package p;
class A{
	void m(){
		/*[*/final int i= 0;/*]*/
		class X{
			int j;
			void m(){
				j= i;
			}
		}
	};
}