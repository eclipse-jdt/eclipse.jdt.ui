//cannot rename to: j
package p;
interface B{
	int j= 0;
}
class A implements B{
	void m(){
		int /*[*/i/*]*/=0;
		int y= j;
	};
}