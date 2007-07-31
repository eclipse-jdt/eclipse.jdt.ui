package p;

import java.util.ArrayList;

public class ControlBodyUpdatesParameter {
	private ArrayList<Integer> arraylists;
	private Integer[] ints;
	private int fNewStart;
	public ControlBodyUpdatesParameter() {
	}
	public ArrayList<Integer> getArraylists() {
		return arraylists;
	}
	public void setArraylists(ArrayList<Integer> arraylists) {
		this.arraylists = arraylists;
	}
	public Integer[] getInts() {
		return ints;
	}
	public void setInts(Integer[] ints) {
		this.ints = ints;
	}
	public int getNewStart() {
		return fNewStart;
	}
	public void setNewStart(int newStart) {
		fNewStart = newStart;
	}
}