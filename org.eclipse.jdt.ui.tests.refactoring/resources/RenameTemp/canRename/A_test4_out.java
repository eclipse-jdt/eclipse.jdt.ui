//rename to: k
package p;
class A{
	int k;
	void m(){
		A /*[*/k/*]*/= new A();
		k.k= k.k;
	}
}