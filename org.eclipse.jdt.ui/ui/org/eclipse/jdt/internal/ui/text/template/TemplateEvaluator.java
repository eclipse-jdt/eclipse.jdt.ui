/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.refactoring.TextUtilities;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.TemplatePreferencePage;
import org.eclipse.jdt.internal.ui.refactoring.changes.TextBuffer;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;

public class TemplateEvaluator implements VariableEvaluator, LinkedPositionUI.CancelListener {
	
	private static final String VARIABLE_POSITION= "TemplateEvaluator.variable.position";
	private static final String MARKER= "/*${cursor}*/";
	private static final IPositionUpdater fgUpdater= new DefaultPositionUpdater(VARIABLE_POSITION);

	private TemplateInterpolator fInterpolator= new TemplateInterpolator();

	private Template fTemplate;
	private TemplateContext fContext;

	private IDocument fDocument;
	
	private int fOldOffset;
	private int fOldLength;
	private String fOldText;
	
	public TemplateEvaluator(Template template, TemplateContext context) {
		fTemplate= template;
		fContext= context;
	}
	
	/*
	 * @see VariableEvaluator#reset()
	 */
	public void reset() {
		fDocument= new Document();
		fDocument.addPositionCategory(VARIABLE_POSITION);
	}

	/*
	 * @see VariableEvaluator#acceptText(String, int)
	 */
	public void acceptText(String text) {
		try {
			int offset= fDocument.getLength();
			fDocument.replace(offset, 0, text);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);	
		}
	}

	/*
	 * @see VariableEvaluator#evaluateVariable(String, int)
	 */
	public String evaluateVariable(String variable) {
		try {
			int offset= fDocument.getLength();
			
			if (variable.equals("cursor")) {
				fDocument.addPosition(VARIABLE_POSITION, new TypedPosition(offset, 0, variable));
				return "";
			} else {				
				fDocument.replace(offset, 0, variable);				
				fDocument.addPosition(VARIABLE_POSITION, new TypedPosition(offset, variable.length(), variable));
				return variable;
			}
		} catch (BadLocationException e) {
			JavaPlugin.log(e);			
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);			
		}

		return null;
	}	

	public String evaluate() {
		String pattern= fTemplate.getPattern();
		return fInterpolator.interpolate(pattern, this);		
	}
	
	private IDocument getDocument(int indentationLevel) {
		String pattern= fTemplate.getPattern();
		fInterpolator.interpolate(pattern, this);

		fDocument.addPositionUpdater(fgUpdater);
		
		format(indentationLevel);

		return fDocument;	
	}

	private void format(int indentationLevel) {
		try {
			if (TemplatePreferencePage.useCodeFormatter() &&
				fContext.getType().equals(TemplateContext.JAVA))
			{
				format2(indentationLevel);
			} else {
				indentate(indentationLevel);
			}
			trimBegin();
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	private Position[] getPositions() {
		try {
			return fDocument.getPositions(VARIABLE_POSITION);
		} catch (BadPositionCategoryException e) {
			return null;
		}
	}

	private int[] getOffsets() {
		Position[] positions= getPositions();
				
		int[] offsets= new int[positions.length];
		for (int i= 0; i != positions.length; i++)
			offsets[i]= positions[i].getOffset();

		return offsets;
	}
	
	private Position getCaretPosition() {
		Position[] positions= getPositions();
		
		for (int i= 0; i != positions.length; i++) {
			if (((TypedPosition) positions[i]).getType().equals("cursor"))
				return positions[i];
		}

		return null;
	}
	
	private int getCaretOffset() {
		Position position= getCaretPosition();
		
		if (position == null)
			return fDocument.getLength();
		else
			return position.getOffset();		
	}
	
	private void update(String string, int[] offsets) throws BadLocationException {
		fDocument.removePositionUpdater(fgUpdater);
		fDocument.replace(0, fDocument.getLength(), string);
		fDocument.addPositionUpdater(fgUpdater);

		try {		
			// update positions ourselves
			Position[] positions= fDocument.getPositions(TemplateEvaluator.VARIABLE_POSITION);
			Assert.isTrue(positions.length == offsets.length);
			for (int i= 0; i != positions.length; i++)
				positions[i].setOffset(offsets[i]);	
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);	
		}
	}

	private void format2(int indentationLevel) throws BadLocationException {
		// XXX 4360
		// workaround for code formatter limitations
		// handle a special case where cursor position is surrounded by whitespaces		

		int caretOffset= getCaretOffset();

		if ((caretOffset > 0) && Character.isWhitespace(fDocument.getChar(caretOffset - 1)) &&
			(caretOffset < fDocument.getLength()) && Character.isWhitespace(fDocument.getChar(caretOffset)))
		{			
			fDocument.replace(caretOffset, 0, MARKER);
			getCaretPosition().setOffset(caretOffset); // reposition caret

			format3(indentationLevel);			
	
			caretOffset= getCaretOffset();
			fDocument.replace(caretOffset, MARKER.length(), "");			
		} else {
			format3(indentationLevel);			
		}
	}

	private void format3(int indentationLevel) throws BadLocationException {
		CodeFormatter formatter= new CodeFormatter(JavaCore.getOptions());
		formatter.setPositionsToMap(getOffsets());
		formatter.setInitialIndentationLevel(indentationLevel);
		String formattedString= formatter.formatSourceString(fDocument.get());	
		
		update(formattedString, formatter.getMappedPositions());
	}
	
	private void indentate(int indentationLevel) throws BadLocationException {
		CodeIndentator indentator= new CodeIndentator();			
		indentator.setIndentationLevel(indentationLevel);			
		indentator.setPositionsToMap(getOffsets());
		String indentatedString= indentator.indentate(fDocument.get());
			
		update(indentatedString, indentator.getMappedPositions());		
	}

	private void trimBegin() throws BadLocationException {
		String string= fDocument.get();

		int i= 0;
		while ((i != string.length()) && Character.isWhitespace(string.charAt(i)))
			i++;
			
		fDocument.replace(0, i, "");
	}
	
	public IRegion apply(IDocument document) {
		int start= fContext.getStart();
		int end= fContext.getEnd();
		
		int indentationLevel= guessIndentationLevel(document, start);
		IDocument template= getDocument(indentationLevel);

		// insert template in document
		try {
			// backup
			fOldText= document.get(start, end - start);
			fOldOffset= start;
			fOldLength= template.get().length();
			
			document.replace(start, end - start, template.get());	
			Position[] positions= getPositions();

			LinkedPositionManager manager= new LinkedPositionManager(document);
			for (int i= 0; i != positions.length; i++) {
				TypedPosition position= (TypedPosition) positions[i];
				
				if (position.getType().equals("cursor"))
					continue;
				
				int offset= position.getOffset() + start;
				int length= position.getLength();
//				String type= position.getType();
				
				manager.addPosition(offset, length);
			}
			
			LinkedPositionUI editor= new LinkedPositionUI(fContext.getViewer(), manager);
			editor.setFinalCaretOffset(getCaretOffset() + start);
			editor.setCancelListener(this);
			editor.enter();

			return editor.getSelectedRegion();

		} catch (BadLocationException e) {
			JavaPlugin.log(e);	
		}
		
		return null;
	}
	
	public void performCancel() {
		IDocument document= fContext.getViewer().getDocument();
		try {
			document.replace(fOldOffset, fOldLength, fOldText);	
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
			// XXX dialog	
		}
	}
	
	private static int guessIndentationLevel(IDocument document, int offset) {
		TextBuffer buffer= new TextBuffer(document);
		String line= buffer.getLineContentOfOffset(offset);
		return TextUtilities.getIndent(line, CodeFormatterPreferencePage.getTabSize());
	}
	
}

