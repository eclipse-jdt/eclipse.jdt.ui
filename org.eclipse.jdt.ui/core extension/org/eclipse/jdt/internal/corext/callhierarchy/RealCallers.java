package org.eclipse.jdt.internal.corext.callhierarchy;

/**
 * Class for the real callers.
 * 
 * @since 3.5
 */
	public class RealCallers extends CallerMethodWrapper {

		/**
		 * Sets the parent method wrapper.
		 * 
		 * @param methodWrapper the method wrapper
		 * @param methodCall the method call
		 */
		public RealCallers(MethodWrapper methodWrapper, MethodCall methodCall) {
			super(methodWrapper, methodCall);
	}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper#canHaveChildren()
		 */
		public boolean canHaveChildren() {
			return true;
	}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper#isRecursive()
		 */
		public boolean isRecursive() {
			return false;
		}
	}
