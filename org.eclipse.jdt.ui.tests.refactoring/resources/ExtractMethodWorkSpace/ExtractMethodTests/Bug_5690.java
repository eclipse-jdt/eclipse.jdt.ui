public class Bug_5690 {
	public void foo() {
		Object runnable= null;
		Object[] disposeList= null;
		for (int i=0; i < disposeList.length; i++) {
			if (disposeList [i] == null) {
				disposeList [i] = runnable;
				return;
			}
		}
	}
}

