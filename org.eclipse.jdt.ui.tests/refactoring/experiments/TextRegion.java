package experiments;

import org.eclipse.jdt.internal.core.refactoring.text.ITextRegion;

class TextRegion implements ITextRegion {

	public int offset;
	public int length;
	
	TextRegion(int offset, int length){
		this.offset= offset;
		this.length= length;
	}
	
	/**
	 * @see ITextRegion#getOffset()
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @see ITextRegion#getLength()
	 */
	public int getLength() {
		return length;
	}
	
	//debugging only
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[offset=");
		buffer.append(this.offset);
		buffer.append(", length=");
		buffer.append(this.length);
		buffer.append("]");
		return buffer.toString();
	}

}

