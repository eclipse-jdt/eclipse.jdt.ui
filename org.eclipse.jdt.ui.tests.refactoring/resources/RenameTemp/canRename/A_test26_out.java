//renaming to: j
package p;
class A{
	int j;
	int m(int k){
		int /*[*/j/*]*/= 0;
		new A(){
			int m(int i){
				return i;
			}
		};
		return j + m(m(j));
	};
}   