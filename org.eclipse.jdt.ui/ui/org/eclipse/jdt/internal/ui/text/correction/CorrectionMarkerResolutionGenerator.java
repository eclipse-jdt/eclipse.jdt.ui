package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

/**
  */
public class CorrectionMarkerResolutionGenerator implements IMarkerResolutionGenerator {

	/**
	 * Constructor for CorrectionMarkerResolutionGenerator.
	 */
	public CorrectionMarkerResolutionGenerator() {
		super();
	}

	/* (non-Javadoc)
	 * @see IMarkerResolutionGenerator#getResolutions(IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker) {
		return new IMarkerResolution[] { new CorrectionMarkerResolution(marker) };
	}
}
