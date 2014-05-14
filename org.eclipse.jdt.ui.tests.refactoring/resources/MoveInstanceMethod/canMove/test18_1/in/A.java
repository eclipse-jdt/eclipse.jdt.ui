package p;
public interface A{
	default String getDefaultName(B b) {
		return "Something";
	}
}