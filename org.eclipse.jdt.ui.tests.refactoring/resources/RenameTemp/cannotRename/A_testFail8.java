//cannot rename to: j
package p;
interface B{
	int j= 0;
}
class A implements B{
	int m(){
		int /*[*/i/*]*/=0;
		i= j;
		return 0;
	};
}