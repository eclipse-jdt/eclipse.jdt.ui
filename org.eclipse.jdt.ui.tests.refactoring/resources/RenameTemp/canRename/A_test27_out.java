//renaming to: j
package p;
class A{
	int j;
	int m(final int k){
		final int /*[*/j/*]*/= 0;
		new A(){
			int m(int o){
				return j;
			}
		};
		return j + m(m(j));
	};
}