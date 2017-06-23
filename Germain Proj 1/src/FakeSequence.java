import java.util.ArrayList;

public class FakeSequence {
	ArrayList<FakeSubpoint> data;
	
	public FakeSequence(ArrayList<FakeSubpoint> data) {
		this.data = data;
	}
	
	public double distance(FakeSequence other) {
		/*
		 * this is the interesting part.
		 * number of identical matched up FakeSubpoints?
		 * same type of FakeSubpoint
		 * For now: two soft distances. Distance between FakeSubpoint and then distance between FakeSequences
		 * Currently ignoring after the end of the shorter FakeSequences
		 * Don't forget to normalize for length of FakeSequences
		 */
		if(this.equals(other)) {
			return 0;
		}
		//this way nothing is 0 from anything but itself. Not sure yet what the exact number should be.
		double distance = 1;
		int size = Math.min(this.data.size(), other.data.size());
		
		for(int i = 0; i < size; i++) {
			FakeSubpoint th = this.data.get(i);
			FakeSubpoint ot = other.data.get(i);
			double subdist = th.distance(ot);
			//System.out.print(subdist + ", ");
			distance += subdist;
		}
		
		distance /= (double)size;
		return distance;
	}
	
	public String toString() {
		String s = "";
		for(FakeSubpoint fs: data) {
			s += fs.toString();
		}
		
		return s;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FakeSequence other = (FakeSequence) obj;
		if (!this.data.equals(other.data))
			return false;
		return true;
	}
}
