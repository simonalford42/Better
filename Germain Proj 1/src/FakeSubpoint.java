import java.util.Arrays;

public class FakeSubpoint implements Comparable<FakeSubpoint> {
	public String[] data;
	public boolean isEmpty;
	public static double[] weights = {0, 		0, 		  1};
	
	public FakeSubpoint(String datalist) {
		this(datalist.split(","));
	}
	
	public FakeSubpoint(String type, int time, String val) {
		String[] data2 = {type, Integer.toString(time), val};
		data = data2;
	}
	
	public FakeSubpoint(String[] data) {
		this.data = data;
	}
	
	public double distance(FakeSubpoint other) {
		int size = Math.min(this.data.length, other.data.length);
		double distance = 0;
		//System.out.print("(");
		//for each attribute
		for (int i = 0; i < size; i++) {
			double attrDist;
			if (this.data[i].equals(other.data[i])) {
				attrDist = 0;
			} else {
				attrDist = weights[i];
			}
			
			distance += attrDist;
		}
		
		//System.out.print(") ");
		return distance;
	}
	
	/**
	 * for sorting clusters of subpoints into time-ordered sequences
	 */
	public int compareTo(FakeSubpoint other) {
		return Integer.compare(Integer.valueOf(this.data[1]), Integer.valueOf(other.data[1]));
	}
	
	public String toString() {
		return data[2];
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FakeSubpoint other = (FakeSubpoint) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		return true;
	}
}
