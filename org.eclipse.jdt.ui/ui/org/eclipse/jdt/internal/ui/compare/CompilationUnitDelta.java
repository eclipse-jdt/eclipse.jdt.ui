package org.eclipse.jdt.internal.ui.compare;

import java.io.InputStream;
import java.util.*;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.compare.structuremergeviewer.*;

/**
 * A <code>CompilationUnitDelta</code> represents the source code changes between
 * a CU in the workspace and the same CU at some point in the past
 * (from the local history).
 * <p>
 * This functionality is used in the context of Hot Code Replace
 * to determine which stack frames are affected (and need to be dropped)
 * by a class reload in the Java VM.
 * <p>
 * Typically a <code>CompilationUnitDelta</code> object is generated for a CU
 * when the associated class is replaced in the VM.
 * The <code>CompilationUnitDelta</code> calculates
 * the differences between the current version of the CU and the version in the
 * local history that was effective at the given <bold>last</bold> build time.
 * The differences are stored as a tree which allows for an efficient implementation
 * of a <code>hasChanged(IMember)</code> method.
 */
public class CompilationUnitDelta {
	
	private static class SimpleJavaElement {
		
		private String fName;
		private HashMap fChildren;
//		private int fChangeType;
		
		SimpleJavaElement(SimpleJavaElement parent, int changeType, String name) {
			fName= name;
//			fChangeType= changeType;
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
	
	private ICompilationUnit fCompilationUnit;
	private SimpleJavaElement fRoot;
	private boolean fHasHistory= false;
	
	/**
	 * Creates a new <code>CompilationUnitDelta object that calculates and stores
	 * the changes of the given CU since some point in time.
	 */
	public CompilationUnitDelta(ICompilationUnit cu, long timestamp) throws CoreException {
		
		if (cu.isWorkingCopy())
			cu= (ICompilationUnit) cu.getOriginalElement();

		fCompilationUnit= cu;
		
		// find underlying file
		IFile file= (IFile) cu.getUnderlyingResource();

		// get available editions
		IFileState[] states= file.getHistory(null);
		if (states == null || states.length <= 0)
			return;
		fHasHistory= true;
		
		IFileState found= null;
		// find edition just before the given time stamp
		for (int i= 0; i < states.length; i++) {
			IFileState state= states[i];
			long d= state.getModificationTime();
			if (d < timestamp) {
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
			return;
		}
		
		JavaStructureCreator jsc= new JavaStructureCreator();
		IStructureComparator oldStructure= jsc.getStructure(oldContents);
		IStructureComparator newStructure= jsc.getStructure(newContents);
		
		final boolean[] memberDeleted= new boolean[1];	// visitor returns result here
		
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
					memberDeleted[0]= true;
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
//		if (fRoot != null)
//			fRoot.dump(0);
			
		if (memberDeleted[0])	// shape change because of deleted members
			fRoot= null;	// throw diffs away since hasChanged(..) must always return true
	}
	
	/**
	 * Returns <code>true</code>
	 * <ul>
	 * <li>if the source of the given member has been changed, or
	 * <li>if the element has been deleted, or
	 * <li>if the element has been newly created
	 * </ul>
	 * after the initial timestamp.
	 * 
	 * @exception AssertionFailedException if member is null or member is not a member of this CU.
	 */
	public boolean hasChanged(IMember member) {
		
		Assert.isNotNull(member);
		ICompilationUnit cu= member.getCompilationUnit();
		if (cu.isWorkingCopy())
			cu= (ICompilationUnit) cu.getOriginalElement();
		Assert.isTrue(cu.equals(fCompilationUnit));
		
		if (fRoot == null) {
			if (fHasHistory)
				return true;	// pessimistic: we have a history but we couldn't use it for some reason
			return false;	// optimistic: we have no history, so assume that member hasn't changed
		}
		
		String[] path= JavaStructureCreator.createPath(member);
		return fRoot.find(path, 0);
	}
}
