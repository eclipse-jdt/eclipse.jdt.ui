/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextPosition;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.ICompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange.TextEditChange;

/**
 * A default content provider to present a hierarchy of <code>IChange</code>
 * objects in a tree viewer.
 */
public class ChangeElementContentProvider  implements ITreeContentProvider {
	
	private static final ChangeElement[] EMPTY_CHILDREN= new ChangeElement[0];
	
	private static class OffsetComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			TextEdit s1= ((TextEditChange)o1).getTextEdit();
			TextEdit s2= ((TextEditChange)o2).getTextEdit();
			int p1= getOffset(s1);
			int p2= getOffset(s2);
			if (p1 < p2)
				return -1;
			if (p1 > p2)
				return 1;
			// same offset
			return 0;	
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
			TextEditChange[] changes=getSortedTextEditChanges(cunitChange);
			for (int i= 0; i < changes.length; i++) {
				TextEditChange tec= changes[i];
				try {
					IJavaElement element= getModifiedJavaElement(tec.getTextEdit(), cunit);
					if (element == cunit) {
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
			TextEditChange[] changes= getSortedTextEditChanges((TextChange)change);
			result= new ChangeElement[changes.length];
			for (int i= 0; i < changes.length; i++) {
				result[i]= new TextEditChangeElement(changeElement, changes[i]);
			}
		}
		changeElement.setChildren(result);
		return result; 
	}
	
	private TextEditChange[] getSortedTextEditChanges(TextChange change) {
		TextEditChange[] result= change.getTextEditChanges();
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
	
	private IJavaElement getModifiedJavaElement(TextEdit edit, ICompilationUnit cunit) throws JavaModelException {
		Object element= edit.getModifiedLanguageElement();
		if (element instanceof IJavaElement)
			return (IJavaElement)element;
		int offset= getOffset(edit);
		if (offset == -1)
			return cunit;
		return cunit.getElementAt(offset);
	}

	private static int getOffset(TextEdit edit) {
		TextPosition[] positions= edit.getTextPositions();
		if (positions == null || positions.length == 0)
			return -1;
		return positions[0].getOffset();
	}	
}

