//compile errors
package p;
class A{
	void m(){
		/*[*/final int i= 0;/*]*/
		List l;
	};
}