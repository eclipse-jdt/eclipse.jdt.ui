/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange.EditChange;

/**
 * A default content provider to present a hierarchy of <code>IChange</code>
 * objects in a tree viewer.
 */
class ChangeElementContentProvider  implements ITreeContentProvider {
	
	private static final ChangeElement[] EMPTY_CHILDREN= new ChangeElement[0];
	
	private static class OffsetComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			EditChange c1= (EditChange)o1;
			EditChange c2= (EditChange)o2;
			int p1= getOffset(c1);
			int p2= getOffset(c2);
			if (p1 < p2)
				return -1;
			if (p1 > p2)
				return 1;
			// same offset
			return 0;	
		}
		private int getOffset(EditChange edit) {
			return edit.getTextRange().getOffset();
		}
	}
	
	/* non Java-doc
	 * @see ITreeContentProvider#inputChanged
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// do nothing
	}
	
	/* non Java-doc
	 * @see ITreeContentProvider#getChildren
	 */
	public Object[] getChildren(Object o) {
		ChangeElement element= (ChangeElement)o;
		ChangeElement[] children= element.getChildren();
		if (children == null) {
			children= createChildren(element);
		}
		return children;
	}
	
	/* non Java-doc
	 * @see ITreeContentProvider#getParent
	 */
	public Object getParent(Object element){
		return ((ChangeElement)element).getParent();
	}
	
	/* non Java-doc
	 * @see ITreeContentProvider#hasChildren
	 */
	public boolean hasChildren(Object element){
		Object[] children= getChildren(element);
		return children != null && children.length > 0;
	}
	
	/* non Java-doc
	 * @see ITreeContentProvider#dispose
	 */
	public void dispose(){
	}
	
	/* non Java-doc
	 * @see ITreeContentProvider#getElements
	 */
	public Object[] getElements(Object element){
		return getChildren(element);
	}
	
	private ChangeElement[] createChildren(ChangeElement object) {
		ChangeElement[] result= EMPTY_CHILDREN;
		if (!(object instanceof DefaultChangeElement))
			return result;
		
		DefaultChangeElement changeElement= (DefaultChangeElement)object;
		IChange change= changeElement.getChange();
		if (change instanceof ICompositeChange) {
			IChange[] children= ((ICompositeChange)change).getChildren();
			result= new ChangeElement[children.length];
			for (int i= 0; i < children.length; i++) {
				result[i]= new DefaultChangeElement(changeElement, children[i]);
			}
		} else if (change instanceof CompilationUnitChange) {
			List children= new ArrayList(5);
			CompilationUnitChange cunitChange= (CompilationUnitChange)change;
			ICompilationUnit cunit= cunitChange.getCompilationUnit();
			Map map= new HashMap(20);
			EditChange[] changes=getSortedTextEditChanges(cunitChange);
			for (int i= 0; i < changes.length; i++) {
				EditChange tec= changes[i];
				try {
					IJavaElement element= getModifiedJavaElement(tec, cunit);
					if (element.equals(cunit)) {
						children.add(new TextEditChangeElement(changeElement, tec));
					} else {
						PseudoJavaChangeElement pjce= getChangeElement(map, element, children, changeElement);
						pjce.addChild(new TextEditChangeElement(pjce, tec));
					}
				} catch (JavaModelException e) {
					children.add(new TextEditChangeElement(changeElement, tec));
				}
			}
			result= (ChangeElement[]) children.toArray(new ChangeElement[children.size()]);
		} else if (change instanceof TextChange) {
			EditChange[] changes= getSortedTextEditChanges((TextChange)change);
			result= new ChangeElement[changes.length];
			for (int i= 0; i < changes.length; i++) {
				result[i]= new TextEditChangeElement(changeElement, changes[i]);
			}
		}
		changeElement.setChildren(result);
		return result; 
	}
	
	private EditChange[] getSortedTextEditChanges(TextChange change) {
		EditChange[] result= change.getTextEditChanges();
		Comparator comparator= new OffsetComparator();
		Arrays.sort(result, comparator);
		return result;
	}
	
	private PseudoJavaChangeElement getChangeElement(Map map, IJavaElement element, List children, ChangeElement cunitChange) {
		PseudoJavaChangeElement result= (PseudoJavaChangeElement)map.get(element);
		if (result != null)
			return result;
		IJavaElement parent= element.getParent();
		if (parent instanceof ICompilationUnit) {
			result= new PseudoJavaChangeElement(cunitChange, element);
			children.add(result);
			map.put(element, result);
		} else {
			PseudoJavaChangeElement parentChange= getChangeElement(map, parent, children, cunitChange);
			result= new PseudoJavaChangeElement(parentChange, element);
			parentChange.addChild(result);
			map.put(element, result);
		}
		return result;
	}
	
	private IJavaElement getModifiedJavaElement(EditChange edit, ICompilationUnit cunit) throws JavaModelException {
		IRegion range= edit.getTextRange();
		if (range.getOffset() == 0 && range.getLength() == 0)
			return cunit;
		IJavaElement result= cunit.getElementAt(range.getOffset());
		if (result == null)
			return cunit;
		
		try {
			while(true) {
				ISourceReference ref= (ISourceReference)result;
				IRegion sRange= new Region(ref.getSourceRange().getOffset(), ref.getSourceRange().getLength());
				if (result.getElementType() == IJavaElement.COMPILATION_UNIT || result.getParent() == null || edit.coveredBy(sRange))
					break;
				result= result.getParent();
			}
		} catch(JavaModelException e) {
			// Do nothing, use old value.
		} catch(ClassCastException e) {
			// Do nothing, use old value.
		}
		return result;
	}
}

