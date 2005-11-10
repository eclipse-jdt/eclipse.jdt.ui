package p;

public interface ISomeIf {
	
	class X {
		ISomeIf fSomeIf;
		ISomeIf fISomeIf;
		/**
		 * @return Returns the iSomeIf.
		 */
		public ISomeIf getISomeIf() {
			return fISomeIf;
		}
		/**
		 * @param someIf The iSomeIf to set.
		 */
		public void setISomeIf(ISomeIf someIf) {
			fISomeIf = someIf;
		}
		/**
		 * @return Returns the someIf.
		 */
		public ISomeIf getSomeIf() {
			return fSomeIf;
		}
		/**
		 * @param someIf The someIf to set.
		 */
		public void setSomeIf(ISomeIf someIf) {
			fSomeIf = someIf;
		}
	}
	
}
