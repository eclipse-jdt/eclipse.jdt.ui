package p;
interface B{

	default String getDefaultName() {
		return "Something";
	}
}