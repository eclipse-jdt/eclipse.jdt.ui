package p; //19, 9, 19, 88

public class A {
	private Object elementName;
	private Object parent;
	
	public Object getElementName() {
		return elementName;
	}

    private class UtilClass {
        public static int combineHashCodes(int a, int b) {
            return a + b;
        }
    }
    @Override
    public int hashCode() {
        int k = this.parent == null ? super.hashCode() :
        UtilClass.combineHashCodes(getElementName().hashCode(), this.parent.hashCode());
        return k;
    }

}