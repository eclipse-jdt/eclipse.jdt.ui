//renaming to: j
package p;
class A{
	A i;
	void m(){
		A /*[*/i/*]*/= null;
		i.toString();
		i.equals(i.toString());
		i.i.i= i;
	};
}