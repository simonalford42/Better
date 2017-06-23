import java.util.Arrays;

public class OldSubpoint implements Comparable<OldSubpoint> {
	private static int idCounter = 0;
	public int id;
	
	public String[] data;
	//public String[] attrLabels;
	//public boolean isEmpty;
	//in the future this should be a separate class for specifying/designing the dataset itself.
	private static double[] weights = {0, 		0, 		  1,  2,	 3, 	4, 		 0, 		0, 		50, 		6};
	//								tStamp, src_addr,   url, cat, subcat, subcat2, method, status, duration, dom-content
	// 0 is for categorical data, 1 is for numerical
	private static int[] compareTypes = {0, 	0,			0,	0, 	0, 		0, 		0, 			0, 		1, 		0};	
	
	private static double maxDist = 0;
	
	static {
		for(double weight: weights) {
			maxDist += weight;
		}
	}
	
	public OldSubpoint(String datalist) {
		this(datalist.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"));
	}
	
	/**
	 * Creates a null subpoint essentially to fill in shorter sequences
	 * to make finding distance between short and long sequences easier.
	 */
	//public Subpoint(int numAttrs) {
		//data = new String[numAttrs];
		//isEmpty = true;
	//}
	
	//also removes the strings around the outside. it'll give an error otherwise
	public OldSubpoint(String[] data) {
		this.id = idCounter;
		idCounter++;
		if (data[0].contains("\"")) {
			for(int i = 0; i < data.length; i++) {
				data[i] = data[i].substring(1,  data[i].length() - 1);
			}
		}
		this.data = data;
	}
	
	public double distance(OldSubpoint other) {
//		if (this.isEmpty) {
//			if(other.isEmpty) {
//				return 0;
//			} else {
//				return maxDist;
//			}
//		} else if (other.isEmpty) {
//			return maxDist;
//		}
			
	   /*  ("TIMESTAMP",	"SOURCE_ADDR",	"URL_PATH",	"CATEGORY",	"SUB_CATEGORY_1",
		* 	"SUB_CATEGORY_2",	"METHOD",	"STATUS",	"DURATION",	"DOM_CONTENT"
		* 	Not now			No				yes			Yes			yes
		* 	yes					yes			yes			yes			yes
		* 
		* 		
		*
		*/
		
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
				//this needs to be adjusted eventually so that <none>'s always give largest distance possible
				//find the largest value and make it that.
				boolean b1 = this.data[i].equals("<none>") || this.data[i].equals("<N/A>");
				boolean b2 = this.data[i].equals("<none>") || other.data[i].equals("<N/A>");
				//System.out.print(" " + b1 + " " + b2 + ", ");
				
				//if both subpoints have numerical values here
				if (!b1 && !b2) {
					double d1 = Double.valueOf(this.data[i]);
					double d2 = Double.valueOf(other.data[i]);
					attrDist = weights[i]*(1.0/FirstKMeans.largestDuration)*Math.abs(Double.valueOf(this.data[i]) - Double.valueOf(other.data[i]));
				} else if (b1 && b2) {
					attrDist = 0;
				} else {
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
	 * for sorting clusters of subpoints into time-ordered sequences
	 * timestamp is subpoint.data[0]. converts it to a Date then compares time ordering
	 */
	public int compareTo(OldSubpoint other) {
		return FirstKMeans.makeDate(this.data[0]).compareTo(FirstKMeans.makeDate(other.data[0]));
	}
	
	public String toString() {
//		if (isEmpty) {
//			return "Empty SP";
//		}
		return Arrays.toString(data);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OldSubpoint other = (OldSubpoint) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		return true;
	}

}
