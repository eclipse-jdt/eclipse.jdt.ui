package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IMarkerResolution;

/**
  */
public class CorrectionMarkerResolution implements IMarkerResolution {

	private IMarker fMarker;

	/**
	 * Constructor for CorrectionMarkerResolution.
	 */
	public CorrectionMarkerResolution(IMarker marker) {
		fMarker= marker;
	}

	/* (non-Javadoc)
	 * @see IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		try {
			return (String) fMarker.getAttribute(IMarker.MESSAGE);
		} catch (CoreException e) {
		}
		return "Exception";
	}

	/* (non-Javadoc)
	 * @see IMarkerResolution#run(IMarker)
	 */
	public void run(IMarker marker) {
	}

}
