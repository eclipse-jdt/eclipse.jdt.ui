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
package org.eclipse.jdt.internal.ui.text.link;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IDocumentExtension.IReplace;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * A <code> LinkedEnvironment</code> umbrellas several <code>PositionGroup</code>s.
 * Responsible for updating the siblings of a linked position when a change
 * occurs.
 * 
 * @since 3.0
 */
public class LinkedEnvironment {

	/**
	 * Returns a linked environment on <code>document</code>.
	 * 
	 * @param document the document for which a <code>LinkedManager</code> is
	 *        requested
	 * @return a linked environment for <code>document</code>
	 */
	public static LinkedEnvironment createLinkedEnvironment(IDocument document) {
		return LinkedManager.createEnvironment(document);
	}

	/**
	 * Returns a linked environment for the specified documents. Throws a
	 * <code>BadLocationException</code> if there is a conflict between the
	 * documents (i.e. two or more environments are already installed on
	 * subsets of <code>documents</code>).
	 * 
	 * @param documents the documents for which a�<code>LinkedManager</code>
	 *        is requested
	 * @return a linked manager for <code>documents</code>
	 * @throws BadLocationException if there is a conflict
	 */
	public static LinkedEnvironment createLinkedEnvironment(IDocument[] documents) throws BadLocationException {
		return LinkedManager.createEnvironment(documents);
	}

	/**
	 * Checks whether there is alreay a linked environment installed on <code>document</code>.
	 * 
	 * @param document the <code>IDocument</code> of interest
	 * @return <code>true</code> if there is an existing environment, <code>false</code>
	 *         otherwise
	 */
	public static boolean hasEnvironment(IDocument document) {
		// if there is a manager, there also is an enviroment
		return LinkedManager.hasManager(document);
	}

	/**
	 * Checks whether there is alreay a linked environment installed on any of
	 * the <code>documents</code>.
	 * 
	 * @param documents the <code>IDocument</code> s of interest
	 * @return <code>true</code> if there is an existing environment, <code>false</code>
	 *         otherwise
	 */
	public static boolean hasEnvironment(IDocument[] documents) {
		// if there is a manager, there also is an enviroment
		return LinkedManager.hasManager(documents);
	}
	
	/**
	 * Cancels any linked environment on the specified document. If there is no 
	 * environment, nothing happens.
	 * 
	 * @param document the document whose <code>LinkedEnvironment</code> should 
	 * 		  be cancelled
	 */
	public static void closeEnvironment(IDocument document) {
		LinkedManager.cancelManager(document);
	}

	/**
	 * Encapsulates the edition triggered by a change to a linked position. Can
	 * be applied to a document as a whole.
	 */
	private class Replace implements IReplace {

		/** The edition to apply on a document. */
		private TextEdit fEdit;

		/**
		 * Creates a new instance.
		 * 
		 * @param edit the edition to apply to a document.
		 */
		public Replace(TextEdit edit) {
			fEdit= edit;
		}

		/*
		 * @see org.eclipse.jface.text.IDocumentExtension.IReplace#perform(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocumentListener)
		 */
		public void perform(IDocument document, IDocumentListener owner) {
			document.removeDocumentListener(owner);
			fIsChanging= true;
			try {
				fEdit.apply(document, TextEdit.UPDATE_REGIONS | TextEdit.CREATE_UNDO);
			} catch (MalformedTreeException e) {
				// log & ignore (can happen on concurrent document modifications
				JavaPlugin.log(new Status(Status.WARNING, JavaPlugin.getPluginId(), Status.OK, "error when applying changes", e)); //$NON-NLS-1$
			} catch (BadLocationException e) {
				// log & ignore (can happen on concurrent document modifications
				JavaPlugin.log(new Status(Status.WARNING, JavaPlugin.getPluginId(), Status.OK, "error when applying changes", e)); //$NON-NLS-1$
			} finally {
				document.addDocumentListener(owner);
				fIsChanging= false;
			}
		}

	}

	/**
	 * The document listener triggering the linked updating of positions
	 * managed by this environment.
	 */
	private class DocumentListener implements IDocumentListener {

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentAboutToBeChanged(DocumentEvent event) {
			// don't react on changes executed by the parent environment
			if (fParentEnvironment != null && fParentEnvironment.isChanging())
				return;

			for (Iterator it= fGroups.iterator(); it.hasNext(); ) {
				LinkedPositionGroup group= (LinkedPositionGroup) it.next();
				if (group.isLegalEvent(event))
					// take the first hit - exlusion is guaranteed by enforcing
					// disjointness when adding positions
					return;
			}
			
			// the event describes a change that lies outside of any managed
			// position -> exit mode.
			// TODO we might not always want to exit, e.g. we want to stay
			// linked if code completion has inserted import statements
			LinkedEnvironment.this.exit(ILinkedListener.EXTERNAL_MODIFICATION);

		}

		/*
		 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
		 */
		public void documentChanged(DocumentEvent event) {
			// don't react on changes executed by the parent environment
			if (fParentEnvironment != null && fParentEnvironment.isChanging())
				return;

			for (Iterator it= fGroups.iterator(); it.hasNext(); ) {
				LinkedPositionGroup group= (LinkedPositionGroup) it.next();

				Map result= group.handleEvent(event);
				if (result == null)
					continue;

				// edit all documents
				for (Iterator it2= result.keySet().iterator(); it2.hasNext(); ) {
					IDocument doc= (IDocument) it2.next();
					TextEdit edit= (TextEdit) result.get(doc);
					Replace replace= new Replace(edit);
				
					// apply the edition, either as post notification replace
					// on the calling document or directly on any other
					// document
					if (doc == event.getDocument()) {
						if (doc instanceof IDocumentExtension) {
							((IDocumentExtension) doc).registerPostNotificationReplace(this, replace);
						} else {
							JavaPlugin.log(new Status(Status.WARNING, JavaPlugin.getPluginId(), Status.OK, "not executing changes on document that is not a IDocumentExtension", null)); //$NON-NLS-1$
						}
					} else {
						replace.perform(doc, this);
					}
				}
				
				// take the first hit - exlusion is guaranteed by enforcing
				// disjointness when adding positions
				return;
			}
		}

	}

	/** The set of linked position groups. */
	private final List fGroups= new ArrayList();
	/** The set of documents spanned by this group. */
	private final Set fDocuments= new HashSet();
	/** The position updater for linked positions. */
	private final IPositionUpdater fUpdater= new InclusivePositionUpdater(getCategory());
	/** The document listener on the documents affected by this environment. */
	private final IDocumentListener fDocumentListener= new DocumentListener();
	/** The parent environment for a hierachical set up, or <code>null</code>. */
	private final LinkedEnvironment fParentEnvironment; // set in ctor
	/**
	 * The position in <code>fParentEnvironment</code> that includes all
	 * positions in this object, or <code>null</code> if there is no parent
	 * environment.
	 */
	private LinkedPosition fParentPosition= null;
	/**
	 * An environment is sealed once it has children - no more positions can be
	 * added.
	 */
	private boolean fIsSealed= false;
	/** <code>true</code> when this environment is changing documents. */
	private boolean fIsChanging= false;
	/** The linked listeners. */
	private final List fListeners= new ArrayList();
	/** Flag telling whether we have exited: */
	private boolean fIsActive= true;
	/**
	 * The sequence of document positions as we are going to iterate through
	 * them.
	 */
	private List fPositionSequence= new ArrayList();

	/**
	 * Creates a new environment. If <code>parent</code> is not <code>null</code>,
	 * a nested environment is created.
	 * 
	 * @param parent the parent environment, or <code>null</code> for a
	 *        top-level environment
	 */
	LinkedEnvironment(LinkedEnvironment parent) {
		fParentEnvironment= parent;
		if (parent != null)
			parent.suspend();
	}

	/**
	 * Whether we are in the process of editing documents (set by <code>Replace</code>,
	 * read by <code>DocumentListener</code>.
	 * 
	 * @return <code>true</code> if we are in the process of editing a
	 *         document, <code>false</code> otherwise
	 */
	private boolean isChanging() {
		return fIsChanging || fParentEnvironment != null && fParentEnvironment.isChanging();
	}

	/**
	 * Throws a <code>BadLocationException</code> if <code>group</code>
	 * conflicts with this environment's groups.
	 * 
	 * @param group the group being checked
	 * @throws BadLocationException if <code>group</code> conflicts with this
	 *         environment's groups
	 */
	private void enforceDisjoint(LinkedPositionGroup group) throws BadLocationException {
		for (Iterator it= fGroups.iterator(); it.hasNext(); ) {
			LinkedPositionGroup g= (LinkedPositionGroup) it.next();
			g.enforceDisjoint(group);
		}
	}

	/**
	 * Causes this environment to exit. Called either if a document change
	 * outside this enviroment is detected, or by the UI.
	 * 
	 * <p>This method part of the private protocol between <code>LinkedUIControl</code> and <code>LinkedEnvironment</code>.</p>
	 * 
	 * @param flags the exit flags.
	 */
	void exit(int flags) {
		if (!fIsActive)
			return;
		fIsActive= false;

		for (Iterator it= fDocuments.iterator(); it.hasNext(); ) {
			IDocument doc= (IDocument) it.next();
			try {
				doc.removePositionCategory(getCategory());
			} catch (BadPositionCategoryException e) {
				// won't happen
				Assert.isTrue(false);
			}
			doc.removePositionUpdater(fUpdater);
			doc.removeDocumentListener(fDocumentListener);
		}

		fDocuments.clear();
		fGroups.clear();

		for (Iterator it= fListeners.iterator(); it.hasNext(); ) {
			ILinkedListener listener= (ILinkedListener) it.next();
			listener.left(this, flags);
		}

		fListeners.clear();

		if (fParentEnvironment != null)
			fParentEnvironment.resume(flags);
	}

	/**
	 * Puts <code>document</code> into the set of managed documents. This
	 * involves registering the document listener and adding our position
	 * category.
	 * 
	 * @param document the new document
	 */
	private void manageDocument(IDocument document) {
		if (!fDocuments.contains(document)) {
			fDocuments.add(document);
			document.addPositionCategory(getCategory());
			document.addPositionUpdater(fUpdater);
			document.addDocumentListener(fDocumentListener);
		}

	}

	/**
	 * Returns the position category used by this environment.
	 * 
	 * @return the position category used by this environment
	 */
	private String getCategory() {
		return toString();
	}

	/**
	 * Adds a position group to this <code>LinkedEnvironment</code>. This
	 * method may not be called if the environment is already sealed, i.e. a
	 * nested environment has been added to it. It is also not wise to add
	 * groups once a UI has been established on top of this environment.
	 * 
	 * <p>
	 * If the positions in <code>group</code> conflict with any other groups
	 * in this environment, a <code>BadLocationException</code> is thrown.
	 * Also, if this environment is nested in another one, all positions in all
	 * groups of the child environment have to lie in a single position in the
	 * parent environment, otherwise a <code>BadLocationException</code> is
	 * thrown.
	 * </p>
	 * 
	 * <p>
	 * If <code>group</code> already exists, nothing happens.
	 * </p>
	 * 
	 * @param group the group to be added to this environment
	 * @throws BadLocationException if the group conflicts with the other
	 *         groups in this environment or violates the nesting requirements.
	 * @throws IllegalStateException if the method is called when the
	 *         environment is already sealed
	 */
	public void addGroup(LinkedPositionGroup group) throws BadLocationException {
		Assert.isNotNull(group);
		if (fIsSealed)
			throw new IllegalStateException("environment is already sealed"); //$NON-NLS-1$
		if (fGroups.contains(group))
			// nothing happens
			return;

		enforceNestability(group);
		enforceDisjoint(group);
		group.setEnvironment(this);
		fGroups.add(group);
	}

	/**
	 * Called by nested environments when a group is added to them. All
	 * positions in all groups of a nested environment have to fit inside a
	 * single position in the parent environment.
	 * 
	 * @param group the group of the nested environment to be adopted.
	 * @throws BadLocationException if the nesting requirement is violated
	 */
	private void enforceNestability(LinkedPositionGroup group) throws BadLocationException {
		if (fParentEnvironment == null)
			return;

		for (Iterator it= fParentEnvironment.fGroups.iterator(); it.hasNext(); ) {
			LinkedPositionGroup pg= (LinkedPositionGroup) it.next();
			LinkedPosition pos= pg.adopt(group);
			if (pos != null && fParentPosition != null && fParentPosition != pos)
				throw new BadLocationException();
			else if (fParentPosition == null && pos != null)
				fParentPosition= pos;
		}

		// group must fit in exactly one of the parent's positions
		if (fParentPosition == null)
			throw new BadLocationException();
	}

	/**
	 * Seals this environment. Further calls to <code>addGroup</code> will
	 * throw an <code>IllegalStateException</code>.
	 */
	void seal() {
		fIsSealed= true;
	}

	/**
	 * Returns whether this environment is nested.
	 * 
	 * <p>This method part of the private protocol between <code>LinkedUIControl</code> and <code>LinkedEnvironment</code>.</p>
	 * 
	 * @return <code>true</code> if this environment is nested, <code>false</code>
	 *         otherwise
	 */
	boolean isNested() {
		return fParentEnvironment != null;
	}

	/**
	 * Returns the positions in this environment that have a tab stop, in the
	 * order they were added.
	 * 
	 * <p>This method part of the private protocol between <code>LinkedUIControl</code> and <code>LinkedEnvironment</code>.</p>
	 * 
	 * @return the positions in this environment that have a tab stop, in the
	 *         order they were added
	 */
	List getTabStopSequence() {
		return fPositionSequence;
	}

	/**
	 * Adds <code>listener</code> to the set of listeners that are informed
	 * upon state changes.
	 * 
	 * @param listener the new listener
	 */
	public void addLinkedListener(ILinkedListener listener) {
		Assert.isNotNull(listener);
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}

	/**
	 * Removes <code>listener</code> from the set of listeners that are
	 * informed upon state changes.
	 * 
	 * @param listener the new listener
	 */
	public void removeLinkedListener(ILinkedListener listener) {
		fListeners.remove(listener);
	}

	/**
	 * Finds the position in this environment that is closest after <code>toFind</code>.
	 * <code>toFind</code> needs not be a position in this environment and
	 * serves merely as an offset.
	 * 
	 * <p>This method part of the private protocol between <code>LinkedUIControl</code> and <code>LinkedEnvironment</code>.</p>
	 * 
	 * @param toFind the position to search from
	 * @return the closest position in the same document as <code>toFind</code>
	 *         after the offset of <code>toFind</code>, or <code>null</code>
	 */
	LinkedPosition findPosition(LinkedPosition toFind) {
		LinkedPosition position= null;
		for (Iterator it= fGroups.iterator(); it.hasNext(); ) {
			LinkedPositionGroup group= (LinkedPositionGroup) it.next();
			position= group.getPosition(toFind);
			if (position != null)
				break;
		}
		return position;
	}

	/**
	 * Registers a <code>LinkedPosition</code> with this environment. Called
	 * by <code>PositionGroup</code>.
	 * 
	 * @param position the position to register
	 * @throws BadLocationException if the position cannot be added to its
	 *         document
	 */
	void register(LinkedPosition position) throws BadLocationException {
		Assert.isNotNull(position);

		IDocument document= position.getDocument();
		manageDocument(document);
		try {
			document.addPosition(getCategory(), position);
		} catch (BadPositionCategoryException e) {
			// won't happen as the category has been added by manageDocument()
			Assert.isTrue(false);
		}
		int seqNr= position.getSequenceNumber();
		if (seqNr != LinkedPositionGroup.NO_STOP) {
			fPositionSequence.add(position);
		}
	}

	/**
	 * Suspends this environment.
	 */
	private void suspend() {
		List l= new ArrayList(fListeners);
		for (Iterator it= l.iterator(); it.hasNext(); ) {
			ILinkedListener listener= (ILinkedListener) it.next();
			listener.suspend(this);
		}
	}

	/**
	 * Resumes this environment. <code>flags</code> can be <code>NONE</code>
	 * or <code>SELECT</code>.
	 * 
	 * @param flags <code>NONE</code> or <code>SELECT</code>
	 */
	private void resume(int flags) {
		List l= new ArrayList(fListeners);
		for (Iterator it= l.iterator(); it.hasNext(); ) {
			ILinkedListener listener= (ILinkedListener) it.next();
			listener.resume(this, flags);
		}
	}

	/**
	 * Returns whether an offset is contained by any position in this
	 * environment.
	 * 
	 * <p>This method part of the private protocol between <code>LinkedUIControl</code> and <code>LinkedEnvironment</code>.</p>
	 * 
	 * @param offset the offset to check
	 * @return <code>true</code> if <code>offset</code> is included by any
	 *         position (see {@link LinkedPosition#includes(int)}) in this
	 *         environment, <code>false</code> otherwise
	 */
	boolean anyPositionContains(int offset) {
		for (Iterator it= fGroups.iterator(); it.hasNext(); ) {
			LinkedPositionGroup group= (LinkedPositionGroup) it.next();
			if (group.contains(offset))
				// take the first hit - exlusion is guaranteed by enforcing
				// disjointness when adding positions
				return true;
		}
		return false;
	}
	
	LinkedPositionGroup getGroupForPosition(LinkedPosition position) {
		for (Iterator it= fGroups.iterator(); it.hasNext(); ) {
			LinkedPositionGroup group= (LinkedPositionGroup) it.next();
			if (group.contains(position))
				return group;
		}
		return null;
	}
}
