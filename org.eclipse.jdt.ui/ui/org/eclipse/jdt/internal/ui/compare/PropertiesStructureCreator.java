/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000, 2001
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.IOException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.*;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.runtime.CoreException;


/**
 * 
 */
public class PropertiesStructureCreator implements IStructureCreator {
	
	static class PropertyNode extends DocumentRangeNode implements ITypedElement {
		
		String fValue;
		
		public PropertyNode(int type, String id, String value, IDocument doc, int start, int length) {
			super(type, id, doc, start, length);
			fValue= value;
		}
				
		/**
		 * @see ITypedElement#getName
		 */
		public String getName() {
			return this.getId();
		}

		/**
		 * @see ITypedElement#getType
		 */
		public String getType() {
			return "txt";
		}
		
		/**
		 * @see ITypedElement#getImage
		 */
		public Image getImage() {
			return CompareUI.getImage(getType());
		}
	};
	
	private static final String whiteSpaceChars= " \t\r\n\f";
	private static final String keyValueSeparators= "=: \t\r\n\f";
	private static final String strictKeyValueSeparators= "=:";
			

	public PropertiesStructureCreator() {
	}
	
	public String getName() {
		return "Property Compare";
	}

	public IStructureComparator getStructure(Object input) {
		
		String s= null;
		if (input instanceof IStreamContentAccessor) {
			try {
				s= JavaCompareUtilities.readString(((IStreamContentAccessor) input).getContents());
			} catch(CoreException ex) {
			}
		}
			
		Document doc= new Document(s != null ? s : "");
				
		PropertyNode root= new PropertyNode(0, "root", "", doc, 0, 0);		
				
		try {
			load(root, doc);
		} catch (IOException ex) {
		}
		
		return root;
	}
	
	public boolean canSave() {
		return true;
	}
	
	public void save(IStructureComparator structure, Object input) {
		if (input instanceof IEditableContent && structure instanceof PropertyNode) {
			IDocument doc= ((PropertyNode)structure).getDocument();
			IEditableContent bca= (IEditableContent) input;
			String c= doc.get();
			bca.setContent(c.getBytes());
		}
	}
		
	public IStructureComparator locate(Object path, Object source) {
		return null;
	}
	
	public boolean canRewriteTree() {
		return false;
	}
	
	public void rewriteTree(Differencer differencer, IDiffContainer root) {
	}
	
	public String getContents(Object node, boolean ignoreWhitespace) {
		if (node instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca= (IStreamContentAccessor) node;
			try {
				return JavaCompareUtilities.readString(sca.getContents());
			} catch (CoreException ex) {
			}
		}
		return null;
	}
	
	private String readLine(int[] args, IDocument doc) {
		int line= args[0]++;
		try {
			IRegion region= doc.getLineInformation(line);
			int start= region.getOffset();
			int length= region.getLength();
			
			try {
				region= doc.getLineInformation(line+1);
				args[1]= region.getOffset();
			} catch (BadLocationException ex) {
				args[1]= doc.getLength();
			}
			
			return doc.get(start, length);
		} catch (BadLocationException ex) {
		}
		return null;
	}
			
	private void load(DocumentRangeNode root, IDocument doc) throws IOException {
		
		int start= 0;
		
		int [] args= new int[2];
		args[0]= 1;
		args[1]= 0;
		
		while (true) {
			// Get next line
            		String line= readLine(args, doc);
			if (line == null)
				return;

			if (line.length() > 0) {
				// Continue lines that end in slashes if they are not comments
				char firstChar= line.charAt(0);
				if ((firstChar != '#') && (firstChar != '!')) {
					
					while (continueLine(line)) {
						String nextLine= readLine(args, doc);
						if (nextLine == null)
							nextLine= new String("");
						String loppedLine= line.substring(0, line.length()-1);
						// Advance beyond whitespace on new line
						int startIndex= 0;
						for (startIndex= 0; startIndex < nextLine.length(); startIndex++)
							if (whiteSpaceChars.indexOf(nextLine.charAt(startIndex)) == -1)
								break;
						nextLine= nextLine.substring(startIndex, nextLine.length());
						line= new String(loppedLine+nextLine);
					}
					
                    		// Find start of key
                    		int len = line.length();
                    		int keyStart;
                    		for (keyStart= 0; keyStart < len; keyStart++) {
                       			if (whiteSpaceChars.indexOf(line.charAt(keyStart)) == -1)
                            			break;
                    		}
                    		
                    		// Find separation between key and value
                    		int separatorIndex;
                    		for (separatorIndex= keyStart; separatorIndex < len; separatorIndex++) {
                        			char currentChar = line.charAt(separatorIndex);
                        			if (currentChar == '\\')
                            			separatorIndex++;
                        			else if(keyValueSeparators.indexOf(currentChar) != -1)
                            			break;
                    		}

                    		// Skip over whitespace after key if any
                    		int valueIndex;
                    		for (valueIndex=separatorIndex; valueIndex<len; valueIndex++)
                        			if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1)
                            			break;

                    		// Skip over one non whitespace key value separators if any
                    		if (valueIndex < len)
                        			if (strictKeyValueSeparators.indexOf(line.charAt(valueIndex)) != -1)
                            			valueIndex++;

                    		// Skip over white space after other separators if any
                    		while (valueIndex < len) {
                        			if (whiteSpaceChars.indexOf(line.charAt(valueIndex)) == -1)
                            			break;
                        			valueIndex++;
                    		}
                    
                    		String key= line.substring(keyStart, separatorIndex);
                    		String value= (separatorIndex < len) ? line.substring(valueIndex, len) : "";

                    		// Convert then store key and value
                    		key= loadConvert(key);
                    		value= loadConvert(value);
                    		
                    		int length= (args[1]-1) - start;
             				root.addChild(new PropertyNode(0, key, value, doc, start, length));
                     
					start= args[1];
				}
			}
		}
	}

	/*
	 * Returns true if the given line is a line that must
	 * be appended to the next line
 	 */
	private boolean continueLine (String line) {
		int slashCount= 0;
		int index= line.length() - 1;
		while((index >= 0) && (line.charAt(index--) == '\\'))
			slashCount++;
		return (slashCount % 2 == 1);
	}

	/*
	 * Converts encoded \\uxxxx to unicode chars
	 * and changes special saved chars to their original forms
	 */
	private String loadConvert(String theString) {
		char aChar;
		int len= theString.length();
		StringBuffer outBuffer= new StringBuffer(len);

		for (int x= 0; x < len; ) {
			aChar = theString.charAt(x++);
			if (aChar == '\\') {
				aChar = theString.charAt(x++);
				if (aChar == 'u') {
					// Read the xxxx
					int value= 0;
					for (int i= 0; i < 4; i++) {
						aChar= theString.charAt(x++);
				        	switch (aChar) {
				          	case '0': case '1': case '2': case '3': case '4':
				          	case '5': case '6': case '7': case '8': case '9':
							value = (value << 4) + aChar - '0';
					     		break;
						case 'a': case 'b': case 'c':
		     				case 'd': case 'e': case 'f':
							value = (value << 4) + 10 + aChar - 'a';
							break;
						case 'A': case 'B': case 'C':
		                    	case 'D': case 'E': case 'F':
							value = (value << 4) + 10 + aChar - 'A';
							break;
						default:
		             				throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
		                        }
					}
					outBuffer.append((char)value);
				} else {
		    			if (aChar == 't')
		    				aChar= '\t';
		       		else if (aChar == 'r')
		       			aChar= '\r';
		            	else if (aChar == 'n')
		            		aChar= '\n';
		          		else if (aChar == 'f')
		          			aChar= '\f';
		            	outBuffer.append(aChar);
				}
			} else
				outBuffer.append(aChar);
		}
		return outBuffer.toString();
	}
}