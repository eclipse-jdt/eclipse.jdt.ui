//rename to k
package p;
class B{
	int i;
}
class A extends B{
	void m(){
		A /*[*/k/*]*/= null;
		super.i= 0;
	}
}