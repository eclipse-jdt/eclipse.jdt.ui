//rename to: j
package p;
class A{
	A i;
	A m(A k){
		A /*[*/j/*]*/= k;
		return j.m(j.m(this.i));
	}
}