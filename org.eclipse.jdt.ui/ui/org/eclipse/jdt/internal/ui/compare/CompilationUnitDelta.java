package org.eclipse.jdt.internal.ui.compare;

import java.io.InputStream;
import java.util.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.compare.structuremergeviewer.*;

/**
 * Object of this class are used to find source code changes of JavaElements
 * that occured after a given point in time.
 */
public class CompilationUnitDelta {
	
	static class SimpleJavaElement {
		
		private String fName;
		private HashMap fChildren;
		private int fChangeType;
		
		SimpleJavaElement(SimpleJavaElement parent, int changeType, String name) {
			fName= name;
			fChangeType= changeType;
			if (parent != null) {
				if (parent.fChildren == null)
					parent.fChildren= new HashMap();
				parent.fChildren.put(name, this);
			}
		}
		
		void dump(int level) {
			for (int i= 0; i < level; i++)	
				System.out.print("  ");
			System.out.println(fName);
			
			if (fChildren != null) {
				Iterator iter= fChildren.values().iterator();
				while (iter.hasNext()) {
					SimpleJavaElement e= (SimpleJavaElement) iter.next();
					e.dump(level+1);
				}
			}
		}
		
		boolean find(String[] path, int start) {
			if (start >= path.length)
				return true;
			String key= path[start];
			if (fChildren != null) {
				SimpleJavaElement child= (SimpleJavaElement) fChildren.get(key);
				if (child != null)
					return child.find(path, start+1);
			}
			return false;
		}
	}
	
	private SimpleJavaElement fRoot;
	
	/**
	 * Creates a new CompilationUnitDelta object that contains
	 * all changed JavaElements of the given CU.
	 */
	public CompilationUnitDelta(ICompilationUnit cu, long timestamp) {
		
		// find underlying file
		IFile file= null;
		try {
			file= (IFile) cu.getUnderlyingResource();
		} catch (JavaModelException ex) {
			JavaPlugin.log(ex);
		}
		if (file == null) {
			System.out.println("can't find underlying resource for " + cu);
			return;
		}

		// get available editions
		IFileState[] states= null;
		try {
			states= file.getHistory(null);
		} catch (CoreException ex) {
			JavaPlugin.log(ex);
		}	
		if (states == null || states.length <= 0) {
			System.out.println("can't get history for " + cu);
			return;
		}
		
		IFileState found= null;
		// find edition just before the given time stamp
		for (int i= 0; i < states.length; i++) {
			IFileState state= states[i];
			if (state.getModificationTime() < timestamp) {
				found= state;
				break;
			}
		}
		
		if (found == null)
			found= states[states.length-1];
		
		InputStream oldContents= null;
		InputStream newContents= null;
		try {
			oldContents= found.getContents();
			newContents= file.getContents();
		} catch (CoreException ex) {
			System.out.println("can't get contents for " + cu);
		}
		
		JavaStructureCreator jsc= new JavaStructureCreator();
		IStructureComparator oldStructure= jsc.getStructure(oldContents);
		IStructureComparator newStructure= jsc.getStructure(newContents);
		
		Differencer differencer= new Differencer() {
			protected Object visit(Object data, int result, Object ancestor, Object left, Object right) {
				String name= null;
				switch (result) {
				case Differencer.CHANGE:
					name= ((JavaNode)left).getId();
					break;
				case Differencer.ADDITION:
					name= ((JavaNode)right).getId();
					break;
				case Differencer.DELETION:
					name= ((JavaNode)left).getId();
					break;
				default:
					break;
				}
				if (name != null)
					return new SimpleJavaElement((SimpleJavaElement) data, result, name);
				return null;
			}
		};
		
		fRoot= (SimpleJavaElement) differencer.findDifferences(false, null, null, null, oldStructure, newStructure);
		if (fRoot != null)
			fRoot.dump(0);
	}
	
	/**
	 * Returns <code>true</code>
	 * <ul>
	 * <li>if the source of the given element has been changed, or
	 * <li>if the element has been deleted, or
	 * <li>if the element has been newly created
	 * </ul>
	 * after the initial timestamp.
	 */
	public boolean hasChanged(IJavaElement element) {
		
		if (fRoot != null) {
			String[] path= JavaStructureCreator.createPath(element);
			return fRoot.find(path, 0);
		}
		return true;
	}
}
