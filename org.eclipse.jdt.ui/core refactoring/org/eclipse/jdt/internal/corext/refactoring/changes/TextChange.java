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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditCopier;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.text.edits.TextEditProcessor;
import org.eclipse.text.edits.UndoEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public abstract class TextChange extends Change {

	public static class EditChange {
		private boolean fIsActive;
		private TextChange fTextChange;
		private TextEditGroup fDescription;
		
		/* package */ EditChange(TextEditGroup description, TextChange change) {
			fTextChange= change;
			fIsActive= true;
			fDescription= description;
		}
		public String getName() {
			return fDescription.getName();
		}
		public void setActive(boolean active) {
			fIsActive= active;
		}
		public boolean isActive() {
			return fIsActive;
		}
		public TextChange getTextChange() {
			return fTextChange;
		}
		public IRegion getTextRange() {
			return fDescription.getRegion();
		}
		public boolean isEmpty() {
			return fDescription.isEmpty();
		}
		/* package */ TextEditGroup getTextEditGroup() {
			return fDescription;
		}
		public boolean coveredBy(IRegion sourceRegion) {
			int sLength= sourceRegion.getLength();
			if (sLength == 0)
				return false;
			int sOffset= sourceRegion.getOffset();
			int sEnd= sOffset + sLength - 1;
			TextEdit[] edits= fDescription.getTextEdits();
			for (int i= 0; i < edits.length; i++) {
				TextEdit edit= edits[i];
				if (edit.isDeleted())
					return false;
				int rOffset= edit.getOffset();
				int rLength= edit.getLength();
				int rEnd= rOffset + rLength - 1;
			    if (rLength == 0) {
					if (!(sOffset < rOffset && rOffset <= sEnd))
						return false;
				} else {
					if (!(sOffset <= rOffset && rEnd <= sEnd))
						return false;
				}
			}
			return true;
		}
	}
	
	protected static class LocalTextEditProcessor extends TextEditProcessor {
		public static final int EXCLUDE= 1;
		public static final int INCLUDE= 2;

		private TextEdit[] fExcludes;
		private TextEdit[] fIncludes;
		
		public LocalTextEditProcessor(IDocument document, TextEdit root, int flags) {
			super(document, root, flags);
		}
		public void setIncludes(TextEdit[] includes) {
			Assert.isNotNull(includes);
			Assert.isTrue(fExcludes == null);
			fIncludes= flatten(includes);
		}
		public void setExcludes(TextEdit[] excludes) {
			Assert.isNotNull(excludes);
			Assert.isTrue(fIncludes == null);
			fExcludes= excludes;
		}
		protected boolean considerEdit(TextEdit edit) {
			if (fExcludes != null) {
				for (int i= 0; i < fExcludes.length; i++) {
					if (edit.equals(fExcludes[i]))
						return false;
				}
				return true;
			}
			if (fIncludes != null) {
				for (int i= 0; i < fIncludes.length; i++) {
					if (edit.equals(fIncludes[i]))
						return true;
				}
				return false;
			}
			return true;
		}
		private TextEdit[] flatten(TextEdit[] edits) {
			List result= new ArrayList(5);
			for (int i= 0; i < edits.length; i++) {
				flatten(result, edits[i]);
			}
			return (TextEdit[])result.toArray(new TextEdit[result.size()]);
		}
		private void flatten(List result, TextEdit edit) {
			result.add(edit);
			TextEdit[] children= edit.getChildren();
			for (int i= 0; i < children.length; i++) {
				flatten(result, children[i]);
			}
		}
	}
	
	private static class PreviewAndRegion {
		public PreviewAndRegion(IDocument d, IRegion r) {
			document= d;
			region= r;
		}
		public IDocument document;
		public IRegion region;
	}
	
	private List fTextEditChanges;
	private TextEditCopier fCopier;
	private TextEdit fEdit;
	private boolean fTrackEdits;
	private String fTextType;
	private IChange fUndoChange;

	/**
	 * A special object denoting all edits managed by the text change. This even 
	 * includes those edits not managed by a <code>TextEditChangeGroup</code> 
	 */
	private static final EditChange[] ALL_EDITS= new EditChange[0]; 
	
	/**
	 * Creates a new text change with the specified name.  The name is a 
	 * human-readable value that is displayed to users.  The name does not 
	 * need to be unique, but it must not be <code>null</code>.
	 * <p>
	 * The text type of this text change is set to <code>txt</code>.
	 * </p>
	 * 
	 * @param name the name of the text change
	 * 
	 * @see #setTextType(String)
	 */
	protected TextChange(String name) {
		super(name);
		fTextEditChanges= new ArrayList(5);
		fTextType= "txt"; //$NON-NLS-1$
	}
	
	/**
	 * Sets the text type. The text type is used to determine the content
	 * merge viewer used to present the difference between the original
	 * and the preview content in the user interface. Content merge viewers
	 * are defined via the extension point <code>org.eclipse.compare.contentMergeViewers</code>.
	 * <p>
	 * The default text type is <code>txt</code>. 
	 * </p>
	 * 
	 * @param type the text type. If <code>null</code> is passed the text type is 
	 *  resetted to the default text type <code>txt</code>.
	 */
	public void setTextType(String type) {
		if (type == null)
			type= "txt"; //$NON-NLS-1$
		fTextType= type;
	}
	
	/**
	 * Returns the text change's text type.
	 * 
	 * @return the text change's text type
	 */
	public String getTextType() {
		return fTextType;
	}
	
	public final RefactoringStatus aboutToPerform(ChangeContext context, IProgressMonitor pm) {
		try {
			return isValid(pm);
		} catch (CoreException e) {
			return RefactoringStatus.createFatalErrorStatus(e.getMessage());
		}
	}
	
	/**
	 * Sets the root text edit that should be applied to the 
	 * document represented by this text change.
	 * 
	 * @param edit the root text edit. The root text edit
	 *  can only be set once. 
	 */
	public void setEdit(TextEdit edit) {
		Assert.isTrue(fEdit == null, "Root edit can only be set once"); //$NON-NLS-1$
		Assert.isTrue(edit != null);
		fEdit= edit;
		fTextEditChanges= new ArrayList(5);
	}
	
	/**
	 * Returns the root text edit.
	 * 
	 * @return the root text edit
	 */
	public TextEdit getEdit() {
		return fEdit;
	}	
	
	/**
	 * Adds a {@link TextEditGroup}. Calling the methods requires that a
	 * root edit has been set via the method {@link #setEdit(TextEdit)}.
	 * The edits managed by the given text edit group must be part of the
	 * change's root edit. 
	 * 
	 * @param group the text edit group to add
	 */
	public void addGroupDescription(TextEditGroup group) {
		Assert.isTrue(fEdit != null, "Can only add a description if a root edit exists"); //$NON-NLS-1$
		Assert.isTrue(group != null);
		fTextEditChanges.add(new EditChange(group, this));
	}
	
	/**
	 * Returns the {@link TextEditGroup}s added to this text change via
	 * the method {@link #addTextEditGroup}.
	 * 
	 * @return the text edit groups added by this text change
	 */	
	public TextEditGroup[] getGroupDescriptions() {
		TextEditGroup[] res= new TextEditGroup[fTextEditChanges.size()];
		for (int i= 0; i < res.length; i++) {
			EditChange elem= (EditChange) fTextEditChanges.get(i);
			res[i]= elem.getTextEditGroup();
		}
		return res;	
	}
	
	/**
	 * Returns the {@link TextEditGroup} with the given name or <code>
	 * null</code> if no such {@link TextEditGroup} exists.
	 * 
	 * @return the text edit group with the given name
	 */	
	public TextEditGroup getGroupDescription(String name) {
		for (int i= 0; i < fTextEditChanges.size(); i++) {
			EditChange elem= (EditChange) fTextEditChanges.get(i);
			TextEditGroup description= elem.getTextEditGroup();
			if (name.equals(description.getName())) {
				return description;
			}
		}
		return null;	
	}	
	
	
	/**
	 * Returns the text edit changes managed by this text change.
	 * 
	 * @return the text edit changes
	 */
	public EditChange[] getTextEditChanges() {
		return (EditChange[])fTextEditChanges.toArray(new EditChange[fTextEditChanges.size()]);
	}
	
	private final IDocument getDocument() throws CoreException {
		IDocument result= aquireDocument(new NullProgressMonitor());
		releaseDocument(result, new NullProgressMonitor());
		return result;
	}
	
	protected abstract IDocument aquireDocument(IProgressMonitor pm) throws CoreException;
	
	protected abstract void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException;
	
	protected abstract IChange createUndoChange(UndoEdit edit) throws CoreException;
	
	/**
	 * {@inheritDoc}
	 */
	public IChange getUndoChange() {
		return fUndoChange;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public final void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		pm.beginTask("", 2); //$NON-NLS-1$
		try {
			IDocument document= null;
			try {
				document= aquireDocument(new SubProgressMonitor(pm, 1));
				TextEditProcessor processor= createTextEditProcessor(document, TextEdit.CREATE_UNDO);
				UndoEdit undo= processor.performEdits();
				fUndoChange= createUndoChange(undo);
			} catch (BadLocationException e) {
				Changes.asCoreException(e);
			} finally {
				if (document != null)
					releaseDocument(document, new SubProgressMonitor(pm, 1));
				pm.done();
			}
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}

	/**
	 * Returns the text this change is working on.
	 * 
	 * @return the original text.
	 * @exception CoreException if text cannot be accessed
	 */
	public String getCurrentContent() throws CoreException {
		return getDocument().get();
	}
	
	/**
	 * 
	 * @param region the region of the document that should be returned. The
	 *  region must denote a valid range in the document represented by this
	 *  text change.
	 * @param expandRegionToFullLine if <code>true</code> is passed the region
	 *  is extended to cover full lines. If <code>false</code> is passed the
	 *  region is unchanged.
	 * @param surroundLines
	 * @return
	 * @throws CoreException
	 */
	public String getCurrentContent(IRegion region, boolean expandRegionToFullLine, int surroundLines) throws CoreException {
		Assert.isNotNull(region);
		Assert.isTrue(surroundLines >= 0);
		IDocument document= getDocument();
		Assert.isTrue(document.getLength() >= region.getOffset() + region.getLength());
		return getContent(document, region, expandRegionToFullLine, 0);
	}

	public IDocument getPreviewDocument() throws CoreException {
		PreviewAndRegion result= getPreviewDocument(ALL_EDITS);
		return result.document;
	}
	
	/**
	 * Returns a preview of the change without actually modifying the 
	 * underlying element.
	 * 
	 * @return the change's preview
	 * @exception CoreException if the preview could not be created
	 */
	public String getPreviewContent() throws CoreException {
		return getPreviewDocument().get();
	}
	
	public String getPreviewContent(EditChange[] changes, IRegion region, boolean expandRegionToFullLine, int surroundingLines) throws CoreException {
		IRegion currentRegion= getRegion(changes);
		Assert.isTrue(region.getOffset() <= currentRegion.getOffset() && 
			currentRegion.getOffset() + currentRegion.getLength() <= region.getOffset() + region.getLength());
		PreviewAndRegion result= getPreviewDocument(changes);
		int delta= result.region.getLength() - currentRegion.getLength();
		return getContent(result.document, new Region(region.getOffset(), region.getLength() + delta), expandRegionToFullLine, surroundingLines);
		
	}

	/**
	 * Controls whether the text change should keep executed edits. If set to <code>true</code>
	 * a call to <code>getExecutedTextEdit(TextEdit original)</code> will return the executed edit
	 * associated with the original edit.
	 * 
	 * @param keep if <code>true</code> executed edits are kept
	 */
	public void setKeepExecutedTextEdits(boolean keep) {
		fTrackEdits= keep;
		if (!fTrackEdits)
			fCopier= null;
	}
	
	/**
	 * Returns the edit that got copied and executed instead of the orignial.
	 * 
	 * @return the executed edit
	 */
	private TextEdit getExecutedTextEdit(TextEdit original) {
		if (!fTrackEdits || fCopier == null)
			return null;
		return fCopier.getCopy(original);
	}
	
	/**
	 * Returns the text range of the given text edit. If the change doesn't 
	 * manage the given text edit or if <code>setTrackPositionChanges</code> 
	 * is set to <code>false</code> <code>null</code> is returned.
	 * 
	 * <p>
	 * Note: API is under construction
	 * </P>
	 */
	public IRegion getNewTextRange(TextEdit edit) {
		Assert.isNotNull(edit);
		TextEdit result= getExecutedTextEdit(edit);
		if (result == null)
			return null;
		return result.getRegion();
	}
	
	/**
	 * Returns the new text range for given text edits. If <code>setTrackPositionChanges</code> 
	 * is set to <code>false</code> <code>null</code> is returned.
	 * 
	 * <p>
	 * Note: API is under construction
	 * </P>
	 */
	public IRegion getNewTextRange(TextEdit[] edits) {
		Assert.isTrue(edits != null && edits.length > 0);
		if (!fTrackEdits || fCopier == null)
			return null;		
		
		TextEdit[] copies= new TextEdit[edits.length];
		for (int i= 0; i < edits.length; i++) {
			TextEdit copy= fCopier.getCopy(edits[i]);
			if (copy == null)
				return null;
			copies[i]= copy;
		}
		return TextEdit.getCoverage(copies);
	}	
	
	/**
	 * Note: API is under construction
	 */
	public IRegion getNewTextRange(EditChange editChange) {
		return getNewTextRange(editChange.getTextEditGroup().getTextEdits());
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void setActive(boolean active) {
		super.setActive(active);
		for (Iterator iter= fTextEditChanges.iterator(); iter.hasNext();) {
			EditChange element= (EditChange) iter.next();
			element.setActive(active);
		}
	}
	
	//---- private helper methods --------------------------------------------------
	
	private PreviewAndRegion getPreviewDocument(EditChange[] changes) throws CoreException {
		IDocument document= new Document(getDocument().get());
		boolean trackChanges= fTrackEdits;
		setKeepExecutedTextEdits(true);
		TextEditProcessor processor= changes == ALL_EDITS
			? createTextEditProcessor(document, TextEdit.NONE)
			: createTextEditProcessor(document, TextEdit.NONE, changes);
		try {
			processor.performEdits();
			return new PreviewAndRegion(document, getNewRegion(changes));
		} catch (BadLocationException e) {
			throw Changes.asCoreException(e);
		} finally {
			setKeepExecutedTextEdits(trackChanges);
		}
	}
	
	private TextEditProcessor createTextEditProcessor(IDocument document, int flags) throws CoreException {
		if (fEdit == null)
			return new TextEditProcessor(document, new MultiTextEdit(0,0), flags);
		List excludes= new ArrayList(0);
		for (Iterator iter= fTextEditChanges.iterator(); iter.hasNext(); ) {
			EditChange edit= (EditChange)iter.next();
			if (!edit.isActive()) {
				excludes.addAll(Arrays.asList(edit.getTextEditGroup().getTextEdits()));
			}
		}
		fCopier= new TextEditCopier(fEdit);
		TextEdit copiedEdit= fCopier.perform();
		if (fTrackEdits)
			flags= flags | TextEdit.UPDATE_REGIONS;
		LocalTextEditProcessor result= new LocalTextEditProcessor(document, copiedEdit, flags);
		result.setExcludes(mapEdits(
			(TextEdit[])excludes.toArray(new TextEdit[excludes.size()]),
			fCopier));	
		if (!fTrackEdits)
			fCopier= null;
		return result;
	}
	
	private TextEditProcessor createTextEditProcessor(IDocument document, int flags, EditChange[] changes) throws CoreException {
		if (fEdit == null)
			return new TextEditProcessor(document, new MultiTextEdit(0,0), flags);
		List includes= new ArrayList(0);
		for (int c= 0; c < changes.length; c++) {
			EditChange change= changes[c];
			Assert.isTrue(change.getTextChange() == this);
			if (change.isActive()) {
				includes.addAll(Arrays.asList(change.getTextEditGroup().getTextEdits()));
			}
		}
		fCopier= new TextEditCopier(fEdit);
		TextEdit copiedEdit= fCopier.perform();
		if (fTrackEdits)
			flags= flags | TextEdit.UPDATE_REGIONS;
		LocalTextEditProcessor result= new LocalTextEditProcessor(document, copiedEdit, flags);
		result.setIncludes(mapEdits(
			(TextEdit[])includes.toArray(new TextEdit[includes.size()]),
			fCopier));
		if (!fTrackEdits)
			fCopier= null;
		return result;
	}
	
	private TextEdit[] mapEdits(TextEdit[] edits, TextEditCopier copier) {
		if (edits == null)
			return null;
		for (int i= 0; i < edits.length; i++) {
			edits[i]= copier.getCopy(edits[i]);
		}
		return edits;
	}
	
	private String getContent(IDocument document, IRegion region, boolean expandRegionToFullLine, int surroundingLines) throws CoreException {
		try {
			if (expandRegionToFullLine) {
				int startLine= Math.max(document.getLineOfOffset(region.getOffset()) - surroundingLines, 0);
				int endLine= Math.min(
					document.getLineOfOffset(region.getOffset() + region.getLength() - 1) + surroundingLines,
					document.getNumberOfLines() - 1);
				
				int offset= document.getLineInformation(startLine).getOffset();
				IRegion endLineRegion= document.getLineInformation(endLine);
				int length = endLineRegion.getOffset() + endLineRegion.getLength() - offset;
				return document.get(offset, length);
				
			} else {
				return document.get(region.getOffset(), region.getLength());
			}
		} catch (BadLocationException e) {
			throw Changes.asCoreException(e);
		}
	}
	
	private IRegion getRegion(EditChange[] changes) {
		if (changes == ALL_EDITS) {
			if (fEdit == null)
				return null;
			return fEdit.getRegion();
		} else {
			List edits= new ArrayList();
			for (int i= 0; i < changes.length; i++) {
				edits.addAll(Arrays.asList(changes[i].getTextEditGroup().getTextEdits()));
			}
			if (edits.size() == 0)
				return null;
			return TextEdit.getCoverage((TextEdit[]) edits.toArray(new TextEdit[edits.size()]));
		}
	}
	
	private IRegion getNewRegion(EditChange[] changes) {
		if (changes == ALL_EDITS) {
			if (fEdit == null)
				return null;
			return fCopier.getCopy(fEdit).getRegion();
		} else {
			List result= new ArrayList();
			for (int c= 0; c < changes.length; c++) {
				TextEdit[] edits= changes[c].getTextEditGroup().getTextEdits();
				for (int e= 0; e < edits.length; e++) {
					TextEdit copy= fCopier.getCopy(edits[e]);
					if (copy != null)
						result.add(copy);
				}
			}
			if (result.size() == 0)
				return null;
			return TextEdit.getCoverage((TextEdit[]) result.toArray(new TextEdit[result.size()]));
		}
	}
}

