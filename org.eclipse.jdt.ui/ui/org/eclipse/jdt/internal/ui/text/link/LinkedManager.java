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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;


/**
 * A linked manager ensures exclusive access of linked position infrastructures to documents. There
 * is at most one <code>LinkedManager</code> installed on the same document. The <code>getManager</code>
 * methods will return the existing instance if any of the specified documents already have an installed
 * manager.
 * 
 * @since 3.0
 */
class LinkedManager {

	/**
	 * Our implementation of <code>ILinkedListener</code>.
	 */
	private class Listener implements ILinkedListener {

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment.ILinkedListener#left(org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment, int)
		 */
		public void left(LinkedEnvironment environment, int flags) {
			LinkedManager.this.left(environment, flags);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment.ILinkedListener#suspend(org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment)
		 */
		public void suspend(LinkedEnvironment environment) {
			// not interested
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment.ILinkedListener#resume(org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment, int)
		 */
		public void resume(LinkedEnvironment environment, int flags) {
			// not interested
		}
		
	}
	
	/** Global map from documents to managers. */
	private static Map fManagers= new HashMap();

	/**
	 * Returns whether there exists a <code>LinkedManager</code> on <code>document</code>.
	 * 
	 * @param document the document of interest
	 * @return <code>true</code> if there exists a <code>LinkedManager</code> on <code>document</code>, <code>false</code> otherwise
	 */
	public static boolean hasManager(IDocument document) {
		return fManagers.get(document) != null;
	}
	
	/**
	 * Returns whether there exists a <code>LinkedManager</code> on any of the <code>documents</code>.
	 * 
	 * @param document the documents of interest
	 * @return <code>true</code> if there exists a <code>LinkedManager</code> on any of the <code>documents</code>, <code>false</code> otherwise
	 */
	public static boolean hasManager(IDocument[] documents) {
		for (int i= 0; i < documents.length; i++) {
			if (hasManager(documents[i]))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns a linked environment on <code>document</code>.
	 * 
	 * @param document the document for which a <code>LinkedManager</code> is requested
	 * @return a linked environment for <code>document</code>
	 */
	public static LinkedEnvironment createEnvironment(IDocument document) {
		LinkedManager mgr= (LinkedManager) fManagers.get(document);
		if (mgr == null) {
			mgr= new LinkedManager();
			fManagers.put(document, mgr);
		}
		return mgr.createLinkedEnvironment();
	}

	/**
	 * Returns the linked manager for the specified documents, or <code>null</code> if there is
	 * a conflict between the documents (i.e. two or more managers are already installed on subsets
	 * of <code>documents</code>).
	 * 
	 * @param documents the documents for which a <code>LinkedManager</code> is requested
	 * @return a linked manager for <code>documents</code>, or <code>null</code> if there is a conflict
	 */
	public static LinkedEnvironment createEnvironment(IDocument[] documents) throws BadLocationException {
		Set mgrs= new HashSet();
		LinkedManager mgr= null;
		for (int i= 0; i < documents.length; i++) {
			mgr= (LinkedManager) fManagers.get(documents[i]);
			if (mgr != null)
				mgrs.add(mgr);
		}
		if (mgrs.size() > 1)
			throw new BadLocationException();
		
		if (mgrs.size() == 0)
			mgr= new LinkedManager();
		
		for (int i= 0; i < documents.length; i++)
			fManagers.put(documents[i], mgr);
		
		return mgr.createLinkedEnvironment();
	}
	
	/**
	 * Cancels any linked manager for the specified document.
	 * 
	 * @param document the document whose <code>LinkedManager</code> should be cancelled
	 */
	public static void cancelManager(IDocument document) {
		LinkedManager mgr= (LinkedManager) fManagers.get(document);
		if (mgr != null)
			mgr.closeAllEnvironments();
	}
	
	/** The hierarchy of environments managed by this manager. */
	private Stack fEnvironments= new Stack();
	private Listener fListener= new Listener();

	/**
	 * Creates a linked environment. If there are already other existing linked environments in the 
	 * manager, a nested linked environment is created. 
	 * 
	 * @return a linked environment on the documents of this manager.
	 */
	public LinkedEnvironment createLinkedEnvironment() {
		LinkedEnvironment env;
		if (fEnvironments.isEmpty()) {
			env= new LinkedEnvironment(null);
		} else {
			LinkedEnvironment parent= (LinkedEnvironment) fEnvironments.peek();
			parent.seal();
			env= new LinkedEnvironment(parent);
		}
		env.addLinkedListener(fListener);
		fEnvironments.push(env);
		return env;
	}
	
	/**
	 * @param environment
	 * @param flags
	 */
	private void left(LinkedEnvironment environment, int flags) {
		if (!fEnvironments.contains(environment))
			return;
		
		while (!fEnvironments.isEmpty()) {
			LinkedEnvironment env= (LinkedEnvironment) fEnvironments.pop();
			if (env == environment)
				break;
			else
				env.exit(ILinkedListener.NONE);
		}
		
		if (fEnvironments.isEmpty()) {
			removeManager();
		}
	}
	
	private void closeAllEnvironments() {
		while (!fEnvironments.isEmpty()) {
			LinkedEnvironment env= (LinkedEnvironment) fEnvironments.pop();
			env.exit(ILinkedListener.NONE);
		}
	
		removeManager();
	}

	private void removeManager() {
		for (Iterator it= fManagers.keySet().iterator(); it.hasNext();) {
			IDocument doc= (IDocument) it.next();
			if (fManagers.get(doc) == this)
				it.remove();
		}
	}
	
}
