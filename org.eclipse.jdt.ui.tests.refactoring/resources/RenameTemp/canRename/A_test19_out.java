//renaming to: j
package p;
class A{
	A i;
	void m(){
		A /*[*/j/*]*/= null;
		j.toString();
		j.equals(j.toString());
		j.i.i= j;
	};
}