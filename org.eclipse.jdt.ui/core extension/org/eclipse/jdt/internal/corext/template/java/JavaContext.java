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
package org.eclipse.jdt.internal.corext.template.java;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.ContextType;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.ITemplateEditor;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateTranslator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.LocalVariable;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * A context for java source.
 */
public class JavaContext extends CompilationUnitContext {

	/** The platform default line delimiter. */
	private static final String PLATFORM_LINE_DELIMITER= System.getProperty("line.separator"); //$NON-NLS-1$

	/** A code completion requestor for guessing local variable names. */
	private CompilationUnitCompletion fCompletion;
	
	/**
	 * Creates a java template context.
	 * 
	 * @param type   the context type.
	 * @param document the document.
	 * @param completionOffset the completion offset within the document.
	 * @param completionLength the completion length.
	 * @param unit the compilation unit (may be <code>null</code>).
	 */
	public JavaContext(ContextType type, IDocument document, int completionOffset, int completionLength,
		ICompilationUnit compilationUnit)
	{
		super(type, document, completionOffset, completionLength, compilationUnit);
	}
	
	/**
	 * Returns the indentation level at the position of code completion.
	 */
	private int getIndentation() {
		int start= getStart();
		IDocument document= getDocument();
		try {
			IRegion region= document.getLineInformationOfOffset(start);
			String lineContent= document.get(region.getOffset(), region.getLength());
			return Strings.computeIndent(lineContent, CodeFormatterUtil.getTabWidth());
		} catch (BadLocationException e) {
			return 0;
		}
	}	
	
	/*
	 * @see TemplateContext#evaluate(Template template)
	 */
	public TemplateBuffer evaluate(Template template) throws CoreException, BadLocationException {

		if (!canEvaluate(template))
			return null;
		
		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(template);

		getContextType().edit(buffer, this);
			
		String lineDelimiter= null;
		try {
			lineDelimiter= getDocument().getLineDelimiter(0);
		} catch (BadLocationException e) {
		}

		if (lineDelimiter == null)
			lineDelimiter= PLATFORM_LINE_DELIMITER;
			
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		boolean useCodeFormatter= prefs.getBoolean(PreferenceConstants.TEMPLATES_USE_CODEFORMATTER);			
		
		ITemplateEditor formatter= new JavaFormatter(lineDelimiter, getIndentation(), useCodeFormatter);
		formatter.edit(buffer, this);

		return buffer;
	}
	
	/*
	 * @see TemplateContext#canEvaluate(Template templates)
	 */
	public boolean canEvaluate(Template template) {
		String key= getKey();

		if (fForceEvaluation)
			return true;

		return
			template.matches(key, getContextType().getName()) &&
			key.length() != 0 && template.getName().toLowerCase().startsWith(key.toLowerCase());
	}

	/*
	 * @see DocumentTemplateContext#getCompletionPosition();
	 */
	public int getStart() {

		try {
			IDocument document= getDocument();

			if (getCompletionLength() == 0) {

				int start= getCompletionOffset();		
				while ((start != 0) && Character.isUnicodeIdentifierPart(document.getChar(start - 1)))
					start--;
					
				if ((start != 0) && Character.isUnicodeIdentifierStart(document.getChar(start - 1)))
					start--;
		
				return start;
			
			} else {

				int start= getCompletionOffset();
				int end= getCompletionOffset() + getCompletionLength();
				
				while (start != 0 && Character.isUnicodeIdentifierPart(document.getChar(start - 1)))
					start--;
				
				while (start != end && Character.isWhitespace(document.getChar(start)))
					start++;
				
				if (start == end)
					start= getCompletionOffset();	
				
				return start;	
			}

		} catch (BadLocationException e) {
			return super.getStart();	
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.DocumentTemplateContext#getEnd()
	 */
	public int getEnd() {
		
		if (getCompletionLength() == 0)		
			return super.getEnd();

		try {			
			IDocument document= getDocument();

			int start= getCompletionOffset();
			int end= getCompletionOffset() + getCompletionLength();
			
			while (start != end && Character.isWhitespace(document.getChar(end - 1)))
				end--;
			
			return end;	

		} catch (BadLocationException e) {
			return super.getEnd();
		}		
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.template.DocumentTemplateContext#getKey()
	 */
	public String getKey() {

		if (getCompletionLength() == 0)		
			return super.getKey();

		try {
			IDocument document= getDocument();

			int start= getStart();
			int end= getCompletionOffset();
			return start <= end
				? document.get(start, end - start)
				: ""; //$NON-NLS-1$
			
		} catch (BadLocationException e) {
			return super.getKey();			
		}
	}

	/**
	 * Returns the character before start position of completion.
	 */
	public char getCharacterBeforeStart() {
		int start= getStart();
		
		try {
			return start == 0
				? ' '
				: getDocument().getChar(start - 1);

		} catch (BadLocationException e) {
			return ' ';
		}
	}

	private static void handleException(Shell shell, Exception e) {
		String title= JavaTemplateMessages.getString("JavaContext.error.title"); //$NON-NLS-1$
		if (e instanceof CoreException)
			ExceptionHandler.handle((CoreException)e, shell, title, null);
		else if (e instanceof InvocationTargetException)
			ExceptionHandler.handle((InvocationTargetException)e, shell, title, null);
		else {
			JavaPlugin.log(e);
			MessageDialog.openError(shell, title, e.getMessage());
		}
	}	

	private CompilationUnitCompletion getCompletion() {
		ICompilationUnit compilationUnit= getCompilationUnit();
		if (fCompletion == null) {
			fCompletion= new CompilationUnitCompletion(compilationUnit);
			
			if (compilationUnit != null) {
				try {
					compilationUnit.codeComplete(getStart(), fCompletion);
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
		
		return fCompletion;
	}

	/**
	 * Returns the name of a guessed local array, <code>null</code> if no local
	 * array exists.
	 */
	public String guessArray() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
				
		if (localArrays.length > 0)
			return localArrays[localArrays.length - 1].name;

		return null;	
	}
	
	/**
	 * Returns the name of the type of a local array, <code>null</code> if no local
	 * array exists.
	 */
	public String guessArrayType() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
				
		if (localArrays.length > 0) {
			LocalVariable localArray= localArrays[localArrays.length - 1];			

			String arrayTypeName= localArray.typeName;
			String typeName= getScalarType(arrayTypeName);
			int dimension= getArrayDimension(arrayTypeName) - 1;
			Assert.isTrue(dimension >= 0);
			
			String qualifiedName= createQualifiedTypeName(localArray.typePackageName, typeName);
			String innerTypeName= completion.simplifyTypeName(qualifiedName);
			
			return innerTypeName == null
				? createArray(typeName, dimension)
				: createArray(innerTypeName, dimension);
		}
		
		return null;
	}
	
	private static String createArray(String type, int dimension) {
		StringBuffer buffer= new StringBuffer(type);
		for (int i= 0; i < dimension; i++)
			buffer.append("[]"); //$NON-NLS-1$
		return buffer.toString();
	}

	private static String getScalarType(String type) {
		return type.substring(0, type.indexOf('['));
	}
	
	private static int getArrayDimension(String type) {

		int dimension= 0;		
		int index= type.indexOf('[');

		while (index != -1) {
			dimension++;
			index= type.indexOf('[', index + 1);	
		}
		
		return dimension;		
	}

	private static String createQualifiedTypeName(String packageName, String className) {
		StringBuffer buffer= new StringBuffer();

		if (packageName.length() != 0) {
			buffer.append(packageName);
			buffer.append('.');
		}
		buffer.append(className);
		
		return buffer.toString();
	}
	
	/**
	 * Returns a proposal for a variable name of a local array element, <code>null</code>
	 * if no local array exists.
	 */
	public String guessArrayElement() {
		ICompilationUnit cu= getCompilationUnit();
		if (cu == null) {
			return null;
		}
		
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
		
		if (localArrays.length > 0) {
			int idx= localArrays.length - 1;
			
			LocalVariable var= localArrays[idx];
			
			IJavaProject project= cu.getJavaProject();
			String typeName= var.typeName;
			String baseTypeName= typeName.substring(0, typeName.lastIndexOf('['));

			String indexName= getIndex();
			String[] excludedNames= completion.getLocalVariableNames();
			if (indexName != null) {
				ArrayList excludedNamesList= new ArrayList(Arrays.asList(excludedNames));
				excludedNamesList.add(indexName);
				excludedNames= (String[])excludedNamesList.toArray(new String[excludedNamesList.size()]);
			}
			String[] proposals= NamingConventions.suggestLocalVariableNames(project, var.typePackageName, baseTypeName, 0, excludedNames);
			if (proposals.length > 0) {
				return proposals[0];
			}
		}

		return null;
	}

	/**
	 * Returns an array index name. 'i', 'j', 'k' are tried until no name collision with
	 * an existing local variable occurs. If all names collide, <code>null</code> is returned.
	 */	
	public String getIndex() {
		CompilationUnitCompletion completion= getCompletion();
		String[] proposals= {"i", "j", "k"};  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		for (int i= 0; i != proposals.length; i++) {
			String proposal = proposals[i];

			if (!completion.existsLocalName(proposal))
				return proposal;
		}

		return null;
	}
	
	/**
	 * Returns the name of a local collection, <code>null</code> if no local collection
	 * exists.
	 */
	public String guessCollection() {
		CompilationUnitCompletion completion= getCompletion();
		try {
			LocalVariable[] localCollections= completion.findLocalCollections();
		
			if (localCollections.length > 0)
				return localCollections[localCollections.length - 1].name;

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}

		return null;
	}

	/**
	 * Returns an iterator name ('iter'). If 'iter' already exists as local variable,
	 * <code>null</code> is returned.
	 */
	public String getIterator() {
		CompilationUnitCompletion completion= getCompletion();		
		String[] proposals= {"iter"}; //$NON-NLS-1$
		
		for (int i= 0; i != proposals.length; i++) {
			String proposal = proposals[i];

			if (!completion.existsLocalName(proposal))
				return proposal;
		}

		return null;
	}


	public void addIteratorImport() {
		ICompilationUnit cu= getCompilationUnit();
		if (cu == null) {
			return;
		}
	
		try {
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			ImportsStructure structure= new ImportsStructure(cu, settings.importOrder, settings.importThreshold, true);
			structure.addImport("java.util.Iterator"); //$NON-NLS-1$
			structure.create(false, null);

		} catch (CoreException e) {
			handleException(null, e);
		}
	}
	
	/**
	 * Evaluates a 'java' template in thecontext of a compilation unit
	 */
	public static String evaluateTemplate(Template template, ICompilationUnit compilationUnit, int position) throws CoreException, BadLocationException {

		ContextType contextType= ContextTypeRegistry.getInstance().getContextType("java"); //$NON-NLS-1$
		if (contextType == null)
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, JavaTemplateMessages.getString("JavaContext.error.message"), null)); //$NON-NLS-1$

		IDocument document= new Document();
		if (compilationUnit != null && compilationUnit.exists())
			document.set(compilationUnit.getSource());

		JavaContext context= new JavaContext(contextType, document, position, 0, compilationUnit);
		context.setForceEvaluation(true);

		TemplateBuffer buffer= context.evaluate(template);
		if (buffer == null)
			return null;
		return buffer.getString();
	}

}

