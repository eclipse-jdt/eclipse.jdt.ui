package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceSourceRangeComputer;

/**
 * A tuple used to keep source of  an element and its type.
 * @see IJavaElement
 * @see ISourceReference
 */
class TypedSource {
	
	private final String fSource;
	private final int fType;

	public TypedSource(String source, int type){
		Assert.isNotNull(source);
		Assert.isTrue(type == IJavaElement.FIELD 
						  || type == IJavaElement.TYPE
						  || type == IJavaElement.IMPORT_CONTAINER
						  || type == IJavaElement.IMPORT_DECLARATION
						  || type == IJavaElement.INITIALIZER
						  || type == IJavaElement.METHOD
						  || type == IJavaElement.PACKAGE_DECLARATION);
		fSource= source;
		fType= type;				  
	}
	
	public TypedSource(ISourceReference ref) throws JavaModelException{
		this(SourceReferenceSourceRangeComputer.computeSource(ref), ((IJavaElement)ref).getElementType());
		Assert.isTrue(((IJavaElement)ref).getElementType() != IJavaElement.CLASS_FILE);
		Assert.isTrue(((IJavaElement)ref).getElementType() != IJavaElement.COMPILATION_UNIT);
	}

	public String getSource() {
		return fSource;
	}

	public int getType() {
		return fType;
	}
}

