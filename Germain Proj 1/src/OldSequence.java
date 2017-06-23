import java.util.ArrayList;
import java.util.Arrays;

public class OldSequence {
	private static int idCounter = 0;
	public int id;
	
	ArrayList<OldSubpoint> data;
	int length;
	
	public OldSequence(ArrayList<OldSubpoint> data) {
		this.data = data;
		this.length = data.size();
		id = idCounter;
		idCounter++;
	}
	
	public double distance(OldSequence other) {
		/*
		 * this is the interesting part.
		 * number of identical matched up subpoints?
		 * same type of subpoint
		 * For now: two soft distances. Distance between subpoint and then distance between sequences
		 * Currently ignoring after the end of the shorter sequences
		 * Don't forget to normalize for length of sequences
		 */
		if(this.equals(other)) {
			return 0;
		}
		//this way nothing is 0 from anything but itself. Not sure yet what the exact number should be.
		double distance = 1;
		
		int size = Math.min(this.data.size(), other.data.size());
		for(int i = 0; i < size; i++) {
			OldSubpoint th = this.data.get(i);
			OldSubpoint ot = other.data.get(i);
			double subdist = th.distance(ot);
			//System.out.print(subdist + ", ");
			distance += subdist;
		}
		
		distance /= (double)size;
		
		return distance;
	}
	
	public String toString() {
		return id + " " + length + " " + data.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OldSequence other = (OldSequence) obj;
		if (!this.data.equals(other.data))
			return false;
		return true;
	}}
