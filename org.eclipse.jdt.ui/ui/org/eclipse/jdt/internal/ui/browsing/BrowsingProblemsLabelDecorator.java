package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.ProblemsLabelDecorator;

import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;

/**
  */
public class BrowsingProblemsLabelDecorator extends ProblemsLabelDecorator {

	/**
	 * Constructor for BrowsingProblemsLabelDecorator.
	 * @param registry
	 */
	public BrowsingProblemsLabelDecorator(ImageDescriptorRegistry registry) {
		super(registry);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.ProblemsLabelDecorator#isInside(int, ISourceReference)
	 */
	protected boolean isInside(int pos, ISourceReference sourceElement) throws CoreException {
		ISourceRange range= sourceElement.getSourceRange();
		int rangeOffset= range.getOffset();
		if (rangeOffset <= pos && rangeOffset + range.getLength() > pos) {
			return true;
		}
		if (pos < rangeOffset && sourceElement instanceof IType) {
			// type also contains problems located in import container
			IType type= (IType) sourceElement;
			ICompilationUnit cu= type.getCompilationUnit();
			return (!type.isMember() && cu != null && type.equals(cu.findPrimaryType()));
		}
		return false;
	}

}
