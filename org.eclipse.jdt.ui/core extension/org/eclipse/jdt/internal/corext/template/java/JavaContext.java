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
package org.eclipse.jdt.internal.corext.template.java;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.jface.text.templates.TemplateVariable;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.LocalVariable;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariable;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * A context for java source.
 */
public class JavaContext extends CompilationUnitContext {

	/** The platform default line delimiter. */
	private static final String PLATFORM_LINE_DELIMITER= System.getProperty("line.separator"); //$NON-NLS-1$

	/** A code completion requester for guessing local variable names. */
	private CompilationUnitCompletion fCompletion;
	
	/**
	 * Creates a java template context.
	 * 
	 * @param type   the context type.
	 * @param document the document.
	 * @param completionOffset the completion offset within the document.
	 * @param completionLength the completion length.
	 * @param compilationUnit the compilation unit (may be <code>null</code>).
	 */
	public JavaContext(TemplateContextType type, IDocument document, int completionOffset, int completionLength,
		ICompilationUnit compilationUnit)
	{
		super(type, document, completionOffset, completionLength, compilationUnit);
	}
	
	/**
	 * Returns the indentation level at the position of code completion.
	 * 
	 * @return the indentation level at the position of the code completion
	 */
	private int getIndentation() {
		int start= getStart();
		IDocument document= getDocument();
		try {
			IRegion region= document.getLineInformationOfOffset(start);
			String lineContent= document.get(region.getOffset(), region.getLength());
			ICompilationUnit compilationUnit= getCompilationUnit();
			IJavaProject project= compilationUnit == null ? null : compilationUnit.getJavaProject();
			return Strings.computeIndent(lineContent, CodeFormatterUtil.getTabWidth(project));
		} catch (BadLocationException e) {
			return 0;
		}
	}	
	
	/*
	 * @see TemplateContext#evaluate(Template template)
	 */
	public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {

		if (!canEvaluate(template))
			throw new TemplateException(JavaTemplateMessages.getString("Context.error.cannot.evaluate")); //$NON-NLS-1$
		
		TemplateTranslator translator= new TemplateTranslator() {
			/*
			 * @see org.eclipse.jface.text.templates.TemplateTranslator#createVariable(java.lang.String, java.lang.String, int[])
			 */
			protected TemplateVariable createVariable(String type, String name, int[] offsets) {
				return new MultiVariable(type, name, offsets);
			}
		};
		TemplateBuffer buffer= translator.translate(template);

		getContextType().resolve(buffer, this);
			
		String lineDelimiter= PLATFORM_LINE_DELIMITER;
		try {
			lineDelimiter= getDocument().getLineDelimiter(0);
		} catch (BadLocationException e) {
			// go on with platform line delimiter
		}
			
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		boolean useCodeFormatter= prefs.getBoolean(PreferenceConstants.TEMPLATES_USE_CODEFORMATTER);			
		
		JavaFormatter formatter= new JavaFormatter(lineDelimiter, getIndentation(), useCodeFormatter);
		formatter.format(buffer, this);

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
			template.matches(key, getContextType().getId()) &&
			key.length() != 0 && template.getName().toLowerCase().startsWith(key.toLowerCase());
	}

	/*
	 * @see DocumentTemplateContext#getCompletionPosition();
	 */
	public int getStart() {

		try {
			IDocument document= getDocument();

			int start= getCompletionOffset();
			int end= getCompletionOffset() + getCompletionLength();
			
			while (start != 0 && Character.isUnicodeIdentifierPart(document.getChar(start - 1)))
				start--;
			
			while (start != end && Character.isWhitespace(document.getChar(start)))
				start++;
			
			if (start == end)
				start= getCompletionOffset();	
			
				return start;	

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
	 * Returns the character before the start position of the completion.
	 * 
	 * @return the character before the start position of the completion
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
	 * Returns the names of local arrays.
	 * 
	 * @return the names of local arrays
	 */
	public String[] getArrays() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
				
		String[] ret= new String[localArrays.length];
		for (int i= 0; i < ret.length; i++) {
			ret[i]= localArrays[i].getName();
		}
		return ret;
	}
	
	/**
	 * Returns the names of the types of the local arrays grouped based on local
	 * variables.
	 * 
	 * @return the names of the types of the local arrays
	 */
	public String[][] getArrayTypes() {
		// TODO propose super types?
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
		
		String[][] ret= new String[localArrays.length][];
		
		for (int i= 0; i < localArrays.length; i++) {
			ret[i]= localArrays[i].getMemberTypeNames();
		}
		
		return ret;
	}
	
	/**
	 * Returns proposals for a variable name of a local array element grouped
	 * based on local array-typed variables.
	 * 
	 * @return proposals for a variable name
	 */
	public String[][] getArrayElements() {
		ICompilationUnit cu= getCompilationUnit();
		if (cu == null) {
			return new String[0][];
		}
		
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();

		return suggestElementNames(localArrays, true);
	}

	/**
	 * Returns an array index name. 'i', 'j', 'k' are tried until no name
	 * collision with an existing local variable occurs. If all names collide,
	 * <code>null</code> is returned.
	 * 
	 * @return a name for an index variable or <code>null</code>
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
	 * Returns the names of local collections.
	 * 
	 * @return the names of local collection
	 */
	public String[] getCollections() {
		CompilationUnitCompletion completion= getCompletion();
		
		LocalVariable[] localCollections= completion.findLocalCollections();
		String[] ret= new String[localCollections.length];
		for (int i= 0; i < ret.length; i++) {
			ret[i]= localCollections[i].getName();
		}
		
		return ret;
	}
	
	/**
	 * Returns the names of local iterables.
	 * 
	 * @return the names of local iterables
	 */
	public String[] getIterables() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localCollections= completion.findLocalIterables();
		String[] ret= new String[localCollections.length];
		for (int i= 0; i < ret.length; i++) {
			ret[i]= localCollections[i].getName();
		}
		
		return ret;
	}
	
	/**
	 * Returns the names of the types of the local iterables grouped based on
	 * local variables.
	 * 
	 * @return the names of the types of the local iterables
	 */
	public String[][] getIterableTypes() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] iterables= completion.findLocalIterables();

		String[][] ret= new String[iterables.length][];

		for (int i= 0; i < iterables.length; i++) {
			ret[i]= iterables[i].getMemberTypeNames();
		}

		return ret;
	}
	
	/**
	 * Returns proposals for a variable name of a local iterable element
	 * grouped based on local array and collection variables.
	 * 
	 * @return proposals for a variable name
	 */
	public String[][] getIterableElements() {
		ICompilationUnit cu= getCompilationUnit();
		if (cu == null) {
			return new String[0][];
		}
		
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] iterables= completion.findLocalIterables();
		
		return suggestElementNames(iterables, false);
	}

	private String[][] suggestElementNames(LocalVariable[] iterables, boolean excludeIndex) throws IllegalArgumentException {
		String[] excludes= computeExcludes(excludeIndex);
		String[][] ret= new String[iterables.length][];
		for (int i= 0; i < iterables.length; i++) {
			ret[i]= suggestVariableName(iterables[i], excludes);
		}
		return ret;
	}

	private String[] computeExcludes(boolean excludeIndex) {
		String[] excludes= getCompletion().getLocalVariableNames();
		if (excludeIndex) {
			String index= getIndex();
			if (index != null) {
				String[] allExcludes= new String[excludes.length + 1];
				System.arraycopy(excludes, 0, allExcludes, 0, excludes.length);
				allExcludes[excludes.length]= index;
				excludes= allExcludes;
			}
		}
		return excludes;
	}

	private String[] suggestVariableName(LocalVariable iterable, String[] excludes) throws IllegalArgumentException {
		IJavaProject project= getCompilationUnit().getJavaProject();
		String memberType= iterable.getMemberTypeSignature();
		String memberPackage= Signature.getSignatureQualifier(memberType);
		String memberTypeName= Signature.getSignatureSimpleName(Signature.getElementType(memberType));
		int memberDimensions= Signature.getArrayCount(memberType);
		String[] proposals= NamingConventions.suggestLocalVariableNames(project, memberPackage, memberTypeName, memberDimensions, excludes);
		return proposals;
	}

	/**
	 * Returns an iterator name ('iter'). If 'iter' already exists as local
	 * variable, <code>null</code> is returned.
	 * 
	 * @return an iterator name or <code>null</code>
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
			Position position= new Position(getCompletionOffset(), getCompletionLength());
			IDocument document= getDocument();
			final String category= "__template_position_importer" + System.currentTimeMillis(); //$NON-NLS-1$
			IPositionUpdater updater= new DefaultPositionUpdater(category);
			document.addPositionCategory(category);
			document.addPositionUpdater(updater);
			document.addPosition(position);

			try {
				
				CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings(cu.getJavaProject());
				ImportsStructure structure= new ImportsStructure(cu, settings.importOrder, settings.importThreshold, true);
				structure.addImport("java.util.Iterator"); //$NON-NLS-1$
				structure.create(false, null);
				
				setCompletionOffset(position.getOffset());
				setCompletionLength(position.getLength());
				
			} catch (CoreException e) {
				handleException(null, e);
			} finally {
				document.removePosition(position);
				document.removePositionUpdater(updater);
				document.removePositionCategory(category);
			}
			
		} catch (BadLocationException e) {
			handleException(null, e);
		} catch (BadPositionCategoryException e) {
			handleException(null, e);
		}
	}
	
	/**
	 * Evaluates a 'java' template in the context of a compilation unit
	 * 
	 * @param template the template to be evaluated
	 * @param compilationUnit the compilation unit in which to evaluate the template
	 * @param position the position inside the compilation unit for which to evaluate the template
	 * @return the evaluated template
	 * @throws CoreException in case the template is of an unknown context type
	 * @throws BadLocationException in case the position is invalid in the compilation unit
	 * @throws TemplateException in case the evaluation fails
	 */
	public static String evaluateTemplate(Template template, ICompilationUnit compilationUnit, int position) throws CoreException, BadLocationException, TemplateException {

		TemplateContextType contextType= JavaPlugin.getDefault().getTemplateContextRegistry().getContextType("java"); //$NON-NLS-1$
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

