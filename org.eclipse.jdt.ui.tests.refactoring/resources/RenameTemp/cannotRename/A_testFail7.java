//cannot rename to: j
package p;
class B{
	int j;
}
class A extends B{
	int m(){
		int /*[*/i/*]*/=0;
		j= 0;
		return 0;
	};
}