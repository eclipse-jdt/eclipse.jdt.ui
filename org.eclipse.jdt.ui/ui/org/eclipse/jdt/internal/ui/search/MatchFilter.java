/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.PatternQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;

abstract class  MatchFilter {
	public abstract boolean isApplicable(JavaSearchQuery query);
	
	public abstract boolean filters(JavaElementMatch match);

	public abstract String getName();
	public abstract String getActionLabel();
	
	public abstract String getDescription();
	
	public abstract String getID();
	
	private static final MatchFilter IMPORT_FILTER= new ImportFilter(); 
	private static final MatchFilter JAVADOC_FILTER= new JavadocFilter(); 
	private static final MatchFilter READ_FILTER= new ReadFilter(); 
	private static final MatchFilter WRITE_FILTER= new WriteFilter(); 
	private static final MatchFilter UNEXACT_FILTER= new UnexactMatchFilter(); 
	private static final MatchFilter ERASURE_FILTER= new ErasureMatchFilter(); 
	
	private static final MatchFilter[] ALL_FILTERS= new MatchFilter[] {
			IMPORT_FILTER,
			JAVADOC_FILTER,
			READ_FILTER,
			WRITE_FILTER,
			UNEXACT_FILTER,
			ERASURE_FILTER
	};
		
	public static MatchFilter[] allFilters() {
		return ALL_FILTERS;
	}	
}

class ImportFilter extends MatchFilter {
	public boolean filters(JavaElementMatch match) {
		return match.getElement() instanceof IImportDeclaration;
	}

	public String getName() {
		return SearchMessages.getString("MatchFilter.ImportFilter.name"); //$NON-NLS-1$
	}

	public String getActionLabel() {
		return SearchMessages.getString("MatchFilter.ImportFilter.actionLabel"); //$NON-NLS-1$
	}

	public String getDescription() {
		return SearchMessages.getString("MatchFilter.ImportFilter.description"); //$NON-NLS-1$
	}
	
	public boolean isApplicable(JavaSearchQuery query) {
		QuerySpecification spec= query.getSpecification();
		if (spec instanceof ElementQuerySpecification) {
			ElementQuerySpecification elementSpec= (ElementQuerySpecification) spec;
			return elementSpec.getElement() instanceof IType;
		} else if (spec instanceof PatternQuerySpecification) {
			PatternQuerySpecification patternSpec= (PatternQuerySpecification) spec;
			return patternSpec.getSearchFor() == IJavaSearchConstants.TYPE;
		}
		return false;
	}

	public String getID() {
		return "filter_imports"; //$NON-NLS-1$
	}
}

abstract class FieldFilter extends MatchFilter {
	public boolean isApplicable(JavaSearchQuery query) {
		QuerySpecification spec= query.getSpecification();
		if (spec instanceof ElementQuerySpecification) {
			ElementQuerySpecification elementSpec= (ElementQuerySpecification) spec;
			return elementSpec.getElement() instanceof IField;
		} else if (spec instanceof PatternQuerySpecification) {
			PatternQuerySpecification patternSpec= (PatternQuerySpecification) spec;
			return patternSpec.getSearchFor() == IJavaSearchConstants.FIELD;
		}
		return false;
	}

}

class WriteFilter extends FieldFilter {
	public boolean filters(JavaElementMatch match) {
		return match.isWriteAccess() && !match.isReadAccess();
	}
	public String getName() {
		return SearchMessages.getString("MatchFilter.WriteFilter.name"); //$NON-NLS-1$
	}
	public String getActionLabel() {
		return SearchMessages.getString("MatchFilter.WriteFilter.actionLabel"); //$NON-NLS-1$
	}
	public String getDescription() {
		return SearchMessages.getString("MatchFilter.WriteFilter.description"); //$NON-NLS-1$
	}
	public String getID() {
		return "filter_writes"; //$NON-NLS-1$
	}
}

class ReadFilter extends FieldFilter {
	public boolean filters(JavaElementMatch match) {
		return match.isReadAccess() && !match.isWriteAccess();
	}
	public String getName() {
		return SearchMessages.getString("MatchFilter.ReadFilter.name"); //$NON-NLS-1$
	}
	public String getActionLabel() {
		return SearchMessages.getString("MatchFilter.ReadFilter.actionLabel"); //$NON-NLS-1$
	}
	public String getDescription() {
		return SearchMessages.getString("MatchFilter.ReadFilter.description"); //$NON-NLS-1$
	}
	public String getID() {
		return "filter_reads"; //$NON-NLS-1$
	}
}

class JavadocFilter extends MatchFilter {
	public boolean filters(JavaElementMatch match) {
		return match.isJavadoc();
	}
	public String getName() {
		return SearchMessages.getString("MatchFilter.JavadocFilter.name"); //$NON-NLS-1$
	}
	public String getActionLabel() {
		return SearchMessages.getString("MatchFilter.JavadocFilter.actionLabel"); //$NON-NLS-1$
	}
	public String getDescription() {
		return SearchMessages.getString("MatchFilter.JavadocFilter.description"); //$NON-NLS-1$
	}
	public boolean isApplicable(JavaSearchQuery query) {
		return true;
	}
	public String getID() {
		return "filter_javadoc"; //$NON-NLS-1$
	}
}

abstract class GenericTypeFilter extends MatchFilter {
	public boolean isApplicable(JavaSearchQuery query) {
		QuerySpecification spec= query.getSpecification();
		if (spec instanceof ElementQuerySpecification) {
			ElementQuerySpecification elementSpec= (ElementQuerySpecification) spec;
			Object element= elementSpec.getElement();
			if (!(element instanceof IType))
				return false;
			ITypeParameter[] typeParameters;
			try {
				typeParameters= ((IType)element).getTypeParameters();
			} catch (JavaModelException e) {
				return false;
			}
			return typeParameters != null && typeParameters.length > 0;
		}
		return false;
	}
}

class ErasureMatchFilter extends GenericTypeFilter {
	public boolean filters(JavaElementMatch match) {
		return (match.getMatchRule() & SearchPattern.R_ERASURE_MATCH) != 0;
	}
	public String getName() {
		return SearchMessages.getString("MatchFilter.ErasureFilter.name"); //$NON-NLS-1$
	}
	public String getActionLabel() {
		return SearchMessages.getString("MatchFilter.ErasureFilter.actionLabel"); //$NON-NLS-1$
	}
	public String getDescription() {
		return SearchMessages.getString("MatchFilter.ErasureFilter.description"); //$NON-NLS-1$
	}
	public String getID() {
		return "filter_erasure"; //$NON-NLS-1$
	}
}

class UnexactMatchFilter extends GenericTypeFilter {
	public boolean filters(JavaElementMatch match) {
		return (match.getMatchRule() & (SearchPattern.R_EQUIVALENT_MATCH | SearchPattern.R_ERASURE_MATCH)) != 0;
	}
	public String getName() {
		return SearchMessages.getString("MatchFilter.UnexactFilter.name"); //$NON-NLS-1$
	}
	public String getActionLabel() {
		return SearchMessages.getString("MatchFilter.UnexactFilter.actionLabel"); //$NON-NLS-1$
	}
	public String getDescription() {
		return SearchMessages.getString("MatchFilter.UnexactFilter.description"); //$NON-NLS-1$
	}
	public String getID() {
		return "filter_unexact"; //$NON-NLS-1$
	}
}

