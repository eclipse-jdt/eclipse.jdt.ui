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
import org.eclipse.jdt.core.search.IJavaSearchConstants;

abstract class  MatchFilter {
	public abstract boolean isApplicable(JavaSearchQuery query);
	
	public abstract boolean filters(JavaElementMatch match);

	public abstract String getName();
	
	public abstract String getDescription();
	
	public abstract String getID();
	
	private static final MatchFilter[] ALL_FILTERS= new MatchFilter[] {
			new ImportFilter(),
			new JavadocFilter(),
			new ReadFilter(),
			new WriteFilter()
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
		} else if (spec instanceof ElementQuerySpecification) {
			PatternQuerySpecification patternSpec= (PatternQuerySpecification) spec;
			return patternSpec.getSearchFor() == IJavaSearchConstants.FIELD;
		}
		return false;
	}

}

class WriteFilter extends FieldFilter {
	public boolean filters(JavaElementMatch match) {
		return match.isWriteAccess();
	}
	public String getName() {
		return SearchMessages.getString("MatchFilter.WriteFilter.name"); //$NON-NLS-1$
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
		return !match.isWriteAccess();
	}
	public String getName() {
		return SearchMessages.getString("MatchFilter.ReadFilter.name"); //$NON-NLS-1$
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


