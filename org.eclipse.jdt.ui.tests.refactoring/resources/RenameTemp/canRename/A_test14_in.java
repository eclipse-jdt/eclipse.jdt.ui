//rename to: j
package p;
class A{
	A i;
	A m(A k){
		A /*[*/i/*]*/= k;
		return i.m(i.m(this.i));
	}
}