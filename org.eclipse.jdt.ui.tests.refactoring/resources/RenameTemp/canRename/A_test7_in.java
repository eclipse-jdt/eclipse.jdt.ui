//rename to k
package p;
class B{
	int i;
}
class A extends B{
	void m(){
		A /*[*/i/*]*/= null;
		super.i= 0;
	}
}