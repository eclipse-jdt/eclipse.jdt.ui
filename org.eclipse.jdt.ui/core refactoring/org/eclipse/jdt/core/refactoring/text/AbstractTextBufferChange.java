/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.core.refactoring.text;

import java.util.ArrayList;import java.util.Collections;import java.util.Comparator;import java.util.Iterator;import java.util.List;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.NullProgressMonitor;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.Change;import org.eclipse.jdt.core.refactoring.IChange;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.core.refactoring.ICompositeChange;import org.eclipse.jdt.core.refactoring.ITextChange;import org.eclipse.jdt.internal.core.refactoring.Assert;import org.eclipse.jdt.internal.core.refactoring.NullChange;

/**
 * A default implementation of <code>ITextBufferChange</code> and <code>ITextChange</code>.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public abstract class AbstractTextBufferChange extends Change implements ITextBufferChange, ITextChange, ICompositeChange {
	
	private static final String LATE_BOUND_STRING= "";
	
	private static class MoveChange extends SimpleReplaceTextChange {
		private int fToOffset;
		
		public MoveChange(String name, int startOffset, int length, int toOffset) {
			super(name, startOffset, length, EMPTY_STRING);
			fToOffset= toOffset;
		}
		
		protected SimpleTextChange[] adjust(ITextBuffer buffer) {
			SimpleReplaceTextChange insert= new SimpleReplaceTextChange(null, fToOffset, 0, 
				buffer.getContent(getOffset(), getLength()));
			return new SimpleTextChange[] { insert };
		}
		
		protected SimpleTextChange createUndoChange(String oldText) {
			return new MoveChange(this.getName(), fToOffset, getLength(), getOffset());
		}		
	}	

	private static class ModificationComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			int p1= ((SimpleTextChange)o1).getOffset();
			int p2= ((SimpleTextChange)o2).getOffset();
			if (p1 < p2)
				return 1;
			if (p1 > p2)
				return -1;
			return 0;	
		}
	}
	
	private static class ReverseComparator implements Comparator {
		private Comparator fComparator;
		
		public ReverseComparator(Comparator comparator) {
			fComparator= comparator;
		}
		public int compare(Object o1, Object o2) {
			return -fComparator.compare(o1, o2);
		}
	}
	
	private String fName;
	private IJavaElement fElement;
	private List fTextModifications;
	private boolean fIsUndoChange;
	private String fCurrentContent;
	private IChange fUndoChange;

	public AbstractTextBufferChange(String name, IJavaElement element) {
		this(name, element, new ArrayList(5), false);
	}
	
	protected AbstractTextBufferChange(String name, IJavaElement element, List modifications, boolean isUndoChange) {
		fTextModifications= new ArrayList(modifications.size());
		for (Iterator iter= modifications.iterator(); iter.hasNext();) {
			addTextModification((SimpleTextChange)iter.next());
		}
		Assert.isNotNull(fTextModifications);
		fName= name;
		Assert.isNotNull(fName);
		fElement= element;
		fIsUndoChange= isUndoChange;
	}
	
	//---- Text buffer management ------------------------------------------
	
	/**
	 * Connects this text change to its text buffer.
	 */
	public abstract void connectTextBuffer() throws CoreException;
	 
	/**
	 * Disconnects this text change from its text buffer.
	 */
	public abstract void disconnectTextBuffer();
	
	/**
	 * Returns the text buffer this text substitue works on.
	 *
	 * @return the new text buffer. Value returns <code>null</code> is the text change
	 * isn't connected to the underlying text buffer (e.g. <code>conntectTextBuffer</code>
	 * has not been called).
	 */
	protected abstract ITextBuffer getTextBuffer();
	
	/**
	 * Save the text buffer this text change works on.
	 *
	 * @param pm a progress monitor to report progress. The value must not be <code>null</code>.
	 */
	protected abstract void saveTextBuffer(IProgressMonitor pm) throws CoreException;
	
	/**
	 * Creates a new text buffer for the given content.
	 *
	 * @param the content of the new text buffer. The content must not be <code>null</code>.
	 * @return the new text buffer.
	 */
	protected abstract ITextBuffer createTextBuffer(String content); 
	 
	/**
	 * Creates a change object for the given list of modifications.
	 *
	 * @param textChanges the list of text changes. Elements in this list are of type
	 *  <code>SimpleTextChange</code>. The value must not be <code>null</code>.
	 * @param isUndo if <code>true</code> then the new change is a undo change. Otherwise
	 *  it is a normal change.
	 * @return the change to be created.
	 */
	protected abstract IChange createChange(List textChanges, boolean isUndo) throws CoreException;	
	
	//---- IChange ----------------------------------------------------------------
		
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void aboutToPerform() {
		for (Iterator iter= fTextModifications.iterator(); iter.hasNext(); ) {
			((IChange)iter.next()).aboutToPerform();
		}
	}
	 
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void performed() {
		for (Iterator iter= fTextModifications.iterator(); iter.hasNext(); ) {
			((IChange)iter.next()).performed();
		}
	}
	 
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public IChange getUndoChange() {
		return fUndoChange;
	}

	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public String getName(){
		if (fIsUndoChange)
			return "Undo " + fName;
		else	
			return fName;
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public IJavaElement getCorrespondingJavaElement(){
		return fElement;
	}

	/* (Non-Javadoc)
	 * Method declared in ICompositeChange.
	 */
	public IChange[] getChildren() {
		Collections.sort(fTextModifications, new ReverseComparator(new ModificationComparator()));
		return (IChange[])fTextModifications.toArray(new IChange[fTextModifications.size()]);
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException {
		try {
			if (isActive()) {				
				connectTextBuffer();
				ITextBuffer buffer= getTextBuffer();
		
				fUndoChange= null;
				
				List undos= internalPerform(fTextModifications, buffer, pm);
				
				saveTextBuffer(pm);
				fUndoChange= createChange(undos, !fIsUndoChange);
			} else {
				fUndoChange= new NullChange();
			}	
		} catch (Exception e) {
			handleException(context, e);
			setActive(false);
			fUndoChange= new NullChange();
		} finally {
			disconnectTextBuffer();
		}
	}

	private List internalPerform(List changes, ITextBuffer textBuffer, IProgressMonitor pm) throws JavaModelException {
		// Make a copy of them.
		changes= new ArrayList(changes);
		
		List result= new ArrayList(changes.size());
		try {
			List newChanges= new ArrayList(0);
			for (Iterator iter= changes.iterator(); iter.hasNext();) {
				SimpleTextChange change= (SimpleTextChange)iter.next();
				SimpleTextChange[] a= change.adjust(textBuffer);
				if (a != null) {
					for (int i= 0; i < a.length; i++) {
						if (a[i] != null)
							newChanges.add(a[i]);
					}
				}
			}
			changes.addAll(newChanges);
			
			pm.beginTask("", changes.size());
			pm.subTask(fName);
			
			sortChanges(changes);
			
			for (Iterator iter= changes.iterator(); iter.hasNext();) {
				SimpleTextChange performed= ((SimpleTextChange)iter.next()).perform(textBuffer);
				if (performed != null)
					result.add(performed);
				pm.worked(1);
			}			
		} finally {
			pm.done();
		}
		return result;
	}
	
	void sortChanges(List changes) {
		Comparator comparator= new ModificationComparator();
		if (fIsUndoChange)
			comparator= new ReverseComparator(comparator);
		Collections.sort(changes, comparator);
	}
	
	//---- ITextBufferChange ------------------------------------------------
	
	/* (Non-Javadoc)
	 * Method declared in ITextBufferChange.
	 */
	public void addReplace(String name, int pos, int length, String text) {
		addTextModification(new SimpleReplaceTextChange(name, pos, length, text));
	}
	
	/* (Non-Javadoc)
	 * Method declared in ITextBufferChange.
	 */
	public void addDelete(String name, int pos, int length) {
		addReplace(name, pos, length, null);
	}
	
	/* (Non-Javadoc)
	 * Method declared in ITextBufferChange.
	 */
	public void addInsert(String name, int pos, String text) {
		addReplace(name, pos, 0, text);
	}
	
	/* (Non-Javadoc)
	 * Method declared in ITextBufferChange.
	 */
	public void addMove(String name, int pos, int length, int to) {
		if (pos == to)
			return;
		addTextModification(new MoveChange(name, pos, length, to));	
	}
	
	/* (Non-Javadoc)
	 * Method declared in ITextBufferChange.
	 */
	public void addSimpleTextChange(SimpleTextChange change) {
		addTextModification(change);
	}

	/* (Non-Javadoc)
	 * Method declared in ITextBufferChange.
	 */
	public boolean isEmpty() {
		return fTextModifications.isEmpty();
	}
	
	private void addTextModification(SimpleTextChange mod) {
		mod.setChange(this);
		fTextModifications.add(mod);
	}

	//---- ITextChange --------------------------------------------------
	
	/* (Non-Javadoc)
	 * Method declared in ITextChange.
	 */
	public String getCurrentContent() throws JavaModelException {
		if (fCurrentContent != null)
			return fCurrentContent;
		try {
			connectTextBuffer();
			fCurrentContent= getTextBuffer().getContent();
			return fCurrentContent;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			disconnectTextBuffer();
		}
	}
	
	/* (Non-Javadoc)
	 * Method declared in ITextChange.
	 */
	public String getPreview() throws JavaModelException {
		if (!isActive())
			return getCurrentContent();

		ITextBuffer buffer= createTextBuffer(getCurrentContent());
		internalPerform(fTextModifications, buffer, new NullProgressMonitor());
		return buffer.getContent();
	}	
}