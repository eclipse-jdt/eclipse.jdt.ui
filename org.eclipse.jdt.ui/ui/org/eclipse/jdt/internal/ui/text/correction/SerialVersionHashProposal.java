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
package org.eclipse.jdt.internal.ui.text.correction;

import java.io.ObjectStreamClass;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Proposal for a hashed serial version id.
 */
public class SerialVersionHashProposal extends SerialVersionDefaultProposal {

	/** The name separator */
	private static final char NAME_SEPARATOR= '.';

	/** The key separator */
	private static final char KEY_SEPARATOR= '/';

	/** The path separator */
	private static final char PATH_SEPARATOR= '/';

	/** The file url protocol */
	private static final String PROTOCOL_FILE= "file://"; //$NON-NLS-1$

	/** The class whose serial version id has to be determined */
	private Class fClass= null;

	/** The serial version id */
	private long fSUID= SERIAL_VALUE;

	/**
	 * Creates a new serial version hash proposal.
	 * 
	 * @param label
	 *        the label of this proposal
	 * @param unit
	 *        the compilation unit
	 * @param node
	 *        the originally selected node
	 */
	public SerialVersionHashProposal(final String label, final ICompilationUnit unit, final ASTNode node) {
		super(label, unit, node);

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

			public final void run() {

				final ASTNode parent= getDeclarationNode();

				ITypeBinding binding= null;
				if (parent instanceof TypeDeclaration) {

					final TypeDeclaration declaration= (TypeDeclaration) parent;
					binding= declaration.resolveBinding();

				} else if (parent instanceof AnonymousClassDeclaration) {

					final AnonymousClassDeclaration declaration= (AnonymousClassDeclaration) parent;
					final ClassInstanceCreation creation= (ClassInstanceCreation) declaration.getParent();

					binding= creation.resolveTypeBinding();
				}

				if (binding != null) {

					final IJavaProject project= unit.getJavaProject();
					final String name= getQualifiedName(binding);

					try {

						final IClasspathEntry[] entries= project.getResolvedClasspath(true);
						final URL[] urls= new URL[entries.length];

						int kind= 0;
						IClasspathEntry entry= null;

						for (int index= 0; index < entries.length; index++) {

							entry= entries[index];
							kind= entry.getEntryKind();

							if (kind == IClasspathEntry.CPE_LIBRARY)
								urls[index]= new URL(PROTOCOL_FILE + entry.getPath().toString());
							else if (kind == IClasspathEntry.CPE_SOURCE)
								urls[index]= new URL(PROTOCOL_FILE + project.getProject().getLocation().makeAbsolute().toString() + project.getOutputLocation().removeFirstSegments(1).makeAbsolute().toFile().toString() + PATH_SEPARATOR);
						}
						fClass= Class.forName(name, false, new URLClassLoader(urls));

					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					} catch (MalformedURLException exception) {
						JavaPlugin.log(exception);
					} catch (ClassNotFoundException exception) {
						// Do nothing
					}
				}
			}
		});
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.SerialVersionDefaultProposal#addLinkedPositions(org.eclipse.jdt.core.dom.rewrite.ASTRewrite, org.eclipse.jdt.core.dom.VariableDeclarationFragment)
	 */
	protected final void addLinkedPositions(final ASTRewrite rewrite, final VariableDeclarationFragment fragment) {
		// Do nothing
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(final IDocument document) {
		super.apply(document);

		fClass= null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.SerialVersionDefaultProposal#canApply()
	 */
	public boolean canApply() {
		return fClass != null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.correction.SerialVersionDefaultProposal#computeDefaultExpression()
	 */
	protected Expression computeDefaultExpression() {
		Assert.isTrue(canApply());

		fSUID= SERIAL_VALUE;

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {

			public final void run() {

				final ObjectStreamClass stream= ObjectStreamClass.lookup(fClass);
				if (stream != null)
					fSUID= stream.getSerialVersionUID();
			}
		});

		return fNode.getAST().newNumberLiteral(fSUID + LONG_SUFFIX);
	}

	/**
	 * Returns the qualified name for the specified type binding.
	 * 
	 * @param binding
	 *        the type binding to get the qualified name for
	 * @return The qualified name of this binding
	 */
	protected final String getQualifiedName(final ITypeBinding binding) {
		Assert.isNotNull(binding);
		
		/*
		 * TODO: Should not rely on format of ITypeBinding#getKey(). 
		 */
		final StringBuffer buffer= new StringBuffer(binding.getKey());
		for (int index= 0; index < buffer.length(); index++) {
	
			if (buffer.charAt(index) == KEY_SEPARATOR)
				buffer.setCharAt(index, NAME_SEPARATOR);
		}
		return buffer.toString();
	}
}
