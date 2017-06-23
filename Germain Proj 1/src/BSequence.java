import java.util.ArrayList;
import java.util.Arrays;

public class BSequence {
	ArrayList<BSubpoint> data;
	
	public BSequence(ArrayList<BSubpoint> data) {
		this.data = data;
	}
	
	public BSequence() {
		this.data = new ArrayList<BSubpoint>();
	}
	
	public void add(BSubpoint bs) {
		data.add(bs);
	}
	
	public double distance(BSequence other) {
		/*
		 * this is the interesting part.
		 * number of identical matched up BSubpoints?
		 * same type of BSubpoint
		 * For now: two soft distances. Distance between BSubpoint and then distance between BSequences
		 * Currently ignoring after the end of the shorter BSequences
		 * Don't forget to normalize for length of BSequences
		 */
		if(this.equals(other)) {
			return 0;
		}
		//this way nothing is 0 from anything but itself. Not sure yet what the exact number should be.
		double distance = 1;
		
		int size = Math.min(this.data.size(), other.data.size());
		for(int i = 0; i < size; i++) {
			BSubpoint th = this.data.get(i);
			BSubpoint ot = other.data.get(i);
			double subdist = th.distance(ot);
			distance += subdist;
		}
		
		//System.out.println("pre " + distance);
		//System.out.println("add " + Math.max(this.data.size(), other.data.size())*BSubpoint.maxDist);
		distance += Math.max(this.data.size(), other.data.size())*BSubpoint.maxDist;
		//distance /= (double)size;
		return distance;
	}
	
	public String toString() {
		return data.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BSequence other = (BSequence) obj;
		if (!this.data.equals(other.data))
			return false;
		return true;
	}}