/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.codeassist.complete.CompletionParser;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemIrritants;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.SearchableEnvironment;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.internal.ui.util.DocumentManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;

public class TemplateContext implements VariableEvaluator {

	private static final String FILE= "file"; //$NON-NLS-1$
	private static final String LINE= "line"; //$NON-NLS-1$
	private static final String DATE= "date"; //$NON-NLS-1$

	private static final String INDEX= "index"; //$NON-NLS-1$
	private static final String ARRAY= "array"; //$NON-NLS-1$
	private static final String ITERATOR= "iterator"; //$NON-NLS-1$
	private static final String COLLECTION= "collection"; //$NON-NLS-1$
	private static final String VECTOR= "vector"; //$NON-NLS-1$
	private static final String ENUMERATION= "enumeration"; //$NON-NLS-1$
	private static final String TYPE= "type"; //$NON-NLS-1$
	private static final String ELEMENT_TYPE= "element_type"; //$NON-NLS-1$
	private static final String ELEMENT= "element"; //$NON-NLS-1$

	private ICompilationUnit fUnit;
	private IDocument fDocument;
	private int fStart;
	private int fEnd;
	private ITextViewer fViewer;

	public TemplateContext(ICompilationUnit unit, int start, int end, ITextViewer viewer) {
		Assert.isTrue(start <= end);
		
		fUnit= unit;
		fStart= start;
		fEnd= end;
		fViewer= viewer;
	}

	public int getStart() {
		return fStart;
	}
	
	public int getEnd() {
		return fEnd;
	}
	
	public ITextViewer getViewer() {
		return fViewer;
	}

	public void setDocument(IDocument document) {
		fDocument= document;
	}
	
	public IDocument getDocument() {
		return fDocument;
	}

	/*
	 * @see VariableEvaluator#reset()
	 */
	public void reset() {
	}

	/*
	 * @see VariableEvaluator#acceptText(String, int)
	 */
	public void acceptText(String variable, int offset) {
	}
		 	 	
	/*
	 * @see VariableEvaluator#evaluateVariable(String, int)
	 */
	public String evaluateVariable(String variable, int offset) {
		if (variable.equals(FILE)) {
			return fUnit.getElementName();

		// line number			
		} else if (variable.equals(LINE)) {
			int line= getLineNumber(fUnit, fEnd);
				
			if (line == -1)
				return null;
			else
				return Integer.toString(line);
			
		} else if (variable.equals(DATE)) {
			return DateFormat.getDateInstance().format(new Date());
		
		} else {
			return null;
		}
	}

	/*
	 * @see VariableEvaluator#getRecognizedVariables()
	 */
	public String[] getRecognizedVariables() {
		return null;
	}

	private static int getLineNumber(ICompilationUnit unit, int offset) {
		CompilationUnitDocumentProvider documentProvider=
			JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
			
		try {
			DocumentManager documentManager= new DocumentManager(unit);
			documentManager.connect();
				
			IDocument document= documentManager.getDocument();
			if (document == null) {
				documentManager.disconnect();
				return -1;	
			}

			try {				
				return document.getLineOfOffset(offset) + 1;
			} catch (BadLocationException e) {
				documentManager.disconnect();
			}
			
		} catch (CoreException e) {
		}
		
		return -1;
	}
}

