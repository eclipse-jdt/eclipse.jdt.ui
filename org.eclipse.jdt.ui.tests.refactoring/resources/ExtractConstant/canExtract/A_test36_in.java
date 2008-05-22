// 7, 20, 7, 25
package p;
enum Bug {
	Z {
		@Override
		String method() {
			return "bug";
		}
	};
}