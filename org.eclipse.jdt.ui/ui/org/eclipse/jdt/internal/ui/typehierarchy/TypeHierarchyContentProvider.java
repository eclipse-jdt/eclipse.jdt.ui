/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.typehierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

/**
 * Base class for content providers for type hierarchy viewers.
 * Implementors must override 'getTypesInHierarchy'.
 * Java delta processing is also performed by the content provider
 */
public abstract class TypeHierarchyContentProvider implements ITreeContentProvider, IElementChangedListener {

	protected static final String[] UNSTRUCTURED= new String[] { IBasicPropertyConstants.P_TEXT, IBasicPropertyConstants.P_IMAGE };
	protected static final Object[] NO_ELEMENTS= new Object[0];

	protected TypeHierarchyLifeCycle fTypeHierarchy;
	protected IMember[] fMemberFilter;
	protected boolean fShowAllTypes;
	
	protected TreeViewer fViewer;
	
	public TypeHierarchyContentProvider(TypeHierarchyLifeCycle lifecycle) {
		fTypeHierarchy= lifecycle;
		fMemberFilter= null;
		fShowAllTypes= false;
	}
	
	/**
	 * Sets members to filter the hierarchy for. Set to null to disable member filtering.
	 * When member filtering is enabled, the hierarchy contains only types that contain
	 * an implementation of one of the filter members and the members themself.
	 * The hierarchy can be empty as well.
	 */
	public void setMemberFilter(IMember[] memberFilter) {
		fMemberFilter= memberFilter;
	}
	
	public IMember[] getMemberFilter() {
		return fMemberFilter;
	}
	
	/**
	 * In member filtering mode, show all types even if they do not contain one
	 * of the filtered members
	 */
	public void showAllTypes(boolean show) {
		fShowAllTypes= show;
	}
	
	
	protected final ITypeHierarchy getHierarchy() {
		return fTypeHierarchy.getHierarchy();
	}

	protected final IType getInputType() {
		return fTypeHierarchy.getInput();
	}
	
	/**
	 * Called for the root element
	 * @see IStructuredContentProvider#getElements	 
	 */
	public Object[] getElements(Object parent) {
		IType input= getInputType();
		if (input == null) {
			return NO_ELEMENTS;
		} else {
			if (fMemberFilter != null) {
				try {
					if (!hasFilteredChildren(input)) {
						return NO_ELEMENTS;
					}
				} catch (JavaModelException e) {
					JavaPlugin.log(e.getStatus());
					return NO_ELEMENTS;
				}
			}		 	
			return new IType[] { input };
		}
	}

	/**
	 * Hook to overwrite. Filter will be applied on the returned types
	 */	
	protected IType[] getTypesInHierarchy(IType type) {
		return new IType[0];
	}	

	/**
	 * Called for the tree children
	 * @see ITreeContentProvider#getChildren
	 */	
	public Object[] getChildren(Object element) {
		if (element instanceof IType) {
			IType type= (IType)element;
			IType[] childrenTypes= getTypesInHierarchy(type);
				
			if (fMemberFilter != null) {
				List children= new ArrayList();
				addFilteredMembers(type, children);
				addFilteredTypes(childrenTypes, children);
				return children.toArray();
			} else {
				return childrenTypes;
			}			
		}
		return NO_ELEMENTS;
	}
	
	/**
	 * @see ITreeContentProvider#hasChildren
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof IType) {
			IType type= (IType)element;
			IType[] childrenTypes= getTypesInHierarchy(type);
				
			if (fMemberFilter != null) {
				List children= new ArrayList();
				addFilteredMembers(type, children);
				addFilteredTypes(childrenTypes, children);
				return children.size() > 0;
			} else {
				return childrenTypes.length > 0;
			}				
		}
		return false;
	}	
	
	protected void addFilteredMembers(IType parent, List children) {
		try {
			IMethod[] methods= parent.getMethods();
			for (int i= 0; i < fMemberFilter.length; i++) {
				IMember member= fMemberFilter[i];
				if (parent.equals(member.getDeclaringType())) {
					children.add(member);
				} else if (member instanceof IMethod) {
					IMethod curr= (IMethod)member;
					IMethod meth= JavaModelUtil.findMethod(curr.getElementName(), curr.getParameterTypes(), curr.isConstructor(), methods);
					if (meth != null) {
						children.add(meth);
					}
				}
			}		
		} catch (JavaModelException e) {
			// ignore
		}
	}
		
	protected void addFilteredTypes(IType[] types, List children) {
		try {
			for (int i= 0; i < types.length; i++) {
				if (hasFilteredChildren(types[i])) {
					children.add(types[i]);
				}
			}
		} catch (JavaModelException e) {
			// ignore
		}
	}
	
	protected boolean hasFilteredChildren(IType type) throws JavaModelException {
		if (fShowAllTypes) {
			return true;
		}
		
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < fMemberFilter.length; i++) {
			IMember member= fMemberFilter[i];
			if (type.equals(member.getDeclaringType())) {
				return true;
			} else if (member instanceof IMethod) {
				IMethod curr= (IMethod)member;
				IMethod meth= JavaModelUtil.findMethod(curr.getElementName(), curr.getParameterTypes(), curr.isConstructor(), methods);
				if (meth != null) {
					return true;
				}
			}
		}
		IType[] childrenTypes= getTypesInHierarchy(type);
		for (int i= 0; i < childrenTypes.length; i++) {
			if (hasFilteredChildren(childrenTypes[i])) {
				return true;
			}
		}
		return false;
	}
	
		
	/**
	 * @see IContentProvider#inputChanged
	 */
	public void inputChanged(Viewer part, Object oldInput, Object newInput) {
		if (part instanceof TreeViewer) {
			fViewer= (TreeViewer)part;
		} else {
			fViewer= null;
		}
		if (oldInput == null && newInput != null) {
			JavaCore.addElementChangedListener(this); 
		} else if (oldInput != null && newInput == null) {
			JavaCore.removeElementChangedListener(this); 
		}
	}
	

	/**
	 * @see IContentProvider#isDeleted
	 */
	public boolean isDeleted(Object obj) {
		try {
			if (obj instanceof IJavaElement) {
				IJavaElement elem= (IJavaElement)obj;
				return !(elem.exists() && JavaModelUtil.isOnBuildPath(elem.getJavaProject(), elem));
			}
		} catch (JavaModelException e) {
			// dont handle here
		}
		return false;
	}

	/**
	 * @see IContentProvider#dispose
	 */	
	public void dispose() {
		// just to be sure
		JavaCore.removeElementChangedListener(this);
	}	

	/**
	 * @see ITreeContentProvider#getParent
	 */
	public Object getParent(Object element) {
		return null;
	}
	
	/**
	 * @see IElementChangedListener#elementChanged
	 */
	public void elementChanged(ElementChangedEvent event) {
		if (getHierarchy() != null) {
			try {
				processDelta(event.getDelta());
			} catch (JavaModelException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
	}
	
	/*
	 * Returns true if the problem has been completly handled
	 */					
	private boolean processDelta(IJavaElementDelta delta) throws JavaModelException {
		IJavaElement element= delta.getElement();
		switch (element.getElementType()) {
			case IJavaElement.JAVA_PROJECT:
				if (!getInputType().getJavaProject().equals(element)) {
					return false;
				}
				break;
			case IJavaElement.COMPILATION_UNIT:
				if (((ICompilationUnit)element).isWorkingCopy()) {
					return false;
				}
				break;
			case IJavaElement.TYPE:
				if (getHierarchy().contains((IType)element)) {
					return processChangeOnType(delta);
				}
				return false;
		}
		IJavaElementDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			if (processDelta(children[i])) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Returns true if the problem has been completly handled
	 */	
	private boolean processChangeOnType(IJavaElementDelta delta) throws JavaModelException {
		IType type= (IType)delta.getElement();
		switch (delta.getKind()) {
			case IJavaElementDelta.REMOVED:
			case IJavaElementDelta.ADDED:
				// type hierarchy change listener
				fViewer.refresh();
				return true;
			case IJavaElementDelta.CHANGED:
				int flags= delta.getFlags();
				if ((flags & IJavaElementDelta.F_CHILDREN) != 0) {
					if (fMemberFilter != null) {
						int nAffected= delta.getAffectedChildren().length;
						int nChanged= delta.getChangedChildren().length;
						if (nAffected - nChanged > 0) {
							fViewer.refresh();
							return false;
						} else {
							fViewer.update(type, UNSTRUCTURED);
						}
					} 
				} else {
					fViewer.update(type, UNSTRUCTURED);
				}
		}
		return false;
	}
	
}