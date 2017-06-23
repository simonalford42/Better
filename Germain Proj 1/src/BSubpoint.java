import java.util.Arrays;

public class BSubpoint implements Comparable {
	public String[] data;
	//public String[] attrLabels;
	public boolean isEmpty;
	//in the future this should be a separate class for specifying/designing the dataset itself.
	private static double[] weights = {0, 		0, 		  0,  			1,  		1,	 	1, 		1, 		 1, 		1, 			1, 				1};
	//						UXT_START_TIME	IPV4_ADDR	USERNAME	URL_PATH	CATEGORY	NAME	NAME2	UXT_METHOD	UXT_STATUS	UXT_DURATION	<N/A>'
	// 0 is for categorical data, 1 is for numerical,
	public static int[] compareTypes = {0,0,0,		0,0,0, 	0,0,0, 	1, 0};	
	
	public static double maxDist = 0;
	
	//the line number of the source file it came from
	public int id;
	
	static {
		for(double weight: weights) {
			maxDist += weight;
		}
	}
	
	public BSubpoint(String datalist) {
		this(datalist.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"));
	}
	
	public BSubpoint(String datalist, int id) {
		this(datalist.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"), id);
	}
	
	/**
	 * Creates a null BSubpoint essentially to fill in shorter sequences
	 * to make finding distance between short and long sequences easier.
	 */
	public BSubpoint(int numAttrs) {
		data = new String[numAttrs];
		isEmpty = true;
	}
	
	public BSubpoint(String[] data) {
		this.id = 0;
		
		if (data[0].contains("\"")) {
			for(int i = 0; i < data.length; i++) {
				data[i] = data[i].substring(1,  data[i].length() - 1);
			}
		}
		this.data = data;
	}
	
	//also removes the strings around the outside. it'll give an error otherwise
	public BSubpoint(String[] data, int id) {
		this.id = id;
		
		if (data[0].contains("\"")) {
			for(int i = 0; i < data.length; i++) {
				data[i] = data[i].substring(1,  data[i].length() - 1);
			}
		}
		this.data = data;
	}
	
	public double distance(BSubpoint other) {
		if (this.isEmpty) {
			if(other.isEmpty) {
				return 0;
			} else {
				return maxDist;
			}
		} else if (other.isEmpty) {
			return maxDist;
		}
		
		int size = Math.min(this.data.length, other.data.length);
		double distance = 0;
		//System.out.print("(");
		//for each attribute
		for (int i = 0; i < size; i++) {
			double attrDist;
			switch(compareTypes[i]) {
			case 0:	
				if (this.data[i].equals(other.data[i])) {
					attrDist = 0;
				} else {
					attrDist = weights[i];
				}

				break;
			case 1:
				boolean b1 = this.data[i].equals("<none>") || this.data[i].equals("<N/A>");
				boolean b2 = this.data[i].equals("<none>") || other.data[i].equals("<N/A>");
				//System.out.print(" " + b1 + " " + b2 + ", ");
				
				//if both BSubpoints have numerical values here
				if (!b1 && !b2) {
					double d1 = Double.valueOf(this.data[i]);
					double d2 = Double.valueOf(other.data[i]);
					attrDist = weights[i]*(1.0/BKMeans.largestDuration)*Math.abs(Double.valueOf(this.data[i]) - Double.valueOf(other.data[i]));
				} else if (b1 && b2) { //if both none, they are 0 distance
					attrDist = 0;
				} else { //one is none, other is numerical. 
					attrDist = weights[i];
				}
				
				break;
				
			default:
				attrDist = 0;
				break;
			}
			
			distance += attrDist;
		}
		
		//System.out.print(") ");
		return distance;
	}
	
	/**
	 * for sorting clusters of BSubpoints into time-ordered sequences
	 * timestamp is BSubpoint.data[0]. converts it to a Date then compares time ordering
	 */
	public int compareTo(BSubpoint other) {
		return BKMeans.makeDate(this.data[0]).compareTo(FirstKMeans.makeDate(other.data[0]));
	}
	
	public String toString() {
		if (isEmpty) {
			return "Empty SP";
		}
		return Arrays.toString(data);
	}
	
	/**
	 * Like how it appeared as a line in the original CSV file when uploaded.
	 * @return
	 */
	public String toOGString() {
		String s = "";
		for(String d: data) {
			s += d + ",";
		}
		s = s.substring(0, s.length() - 1);
		return s;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BSubpoint other = (BSubpoint) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		return true;
	}

	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}
}
