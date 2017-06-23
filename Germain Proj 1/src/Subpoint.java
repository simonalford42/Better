import java.util.Arrays;

public class Subpoint implements Comparable<Subpoint> {
	public static int idCounter = 0;
	public int id;
	public int equalsId;
	public String label;
	public String[] data;
	public String[] equalsData;
	
	public Subpoint(String datalist) {
		this(datalist.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"));
	}
	
	//also removes the strings around the outside. it'll give an error otherwise
	public Subpoint(String[] data) {
		this.id = idCounter;
		idCounter++;
		
		equalsData = new String[FreshK.equalsLength];
		int i2 = 0;
		for(int i = 0; i < data.length; i++) {
			if (data[i].length() > 0 && data[i].charAt(0) == '"')
				data[i] = data[i].substring(1,  data[i].length() - 1);
			
			if (FreshK.weights[i] != 0) {
				equalsData[i2] = data[i];
				i2++;
			}
		}
		
		this.data = data;
	}
	
	
	public double distance(Subpoint other) {
		//System.out.println();
		int size = this.data.length;
		double distance = 0;

		//for each attribute
		for (int i = 0; i < size; i++) {
			if (FreshK.weights[i] == 0) {
				continue;
			}
			double attrDist;
			switch(FreshK.compareTypes[i]) {
			case 0:	
				if (this.data[i].equals(other.data[i])) {
					attrDist = 0;
				} else {
					attrDist = FreshK.weights[i];
				}
				break;
			case 1:
				boolean b1 = this.data[i].equals("<none>") || this.data[i].equals("<N/A>");
				boolean b2 = this.data[i].equals("<none>") || other.data[i].equals("<N/A>");
				
				//if both subpoints have numerical values here
				if (!b1 && !b2) {
					double d1 = Double.valueOf(this.data[i]);
					double d2 = Double.valueOf(other.data[i]);
					attrDist = FreshK.weights[i]*(1.0/FreshK.rangeNumVals[i])*Math.abs(d1 - d2);
				} else if (b1 && b2) {
					attrDist = 0;
				} else {
					attrDist = FreshK.weights[i];
				}
				break;
			case 2:
				String s1 = this.data[i];
				String s2 = other.data[i];
				attrDist = FreshK.weights[i]*FreshK.stringDist(s1, s2); 
				break;
			default:
				attrDist = 0;
				throw new java.lang.RuntimeException("this not right");
			}
			//System.out.print(i + ":" + attrDist + " ");
			distance += attrDist;
		}
		return distance;
	}
	
	/**
	 * for sorting clusters of subpoints into time-ordered sequences
	 * timestamp is subpoint.data[0]. converts it to a Date then compares time ordering
	 */
	public int compareTo(Subpoint other) {
		return FreshK.dateParser.makeDate(this.data[0]).compareTo(FreshK.dateParser.makeDate(other.data[0]));
	}
	
	public String toString() {
		return Arrays.toString(equalsData);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(equalsData);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subpoint other = (Subpoint) obj;
		if (!Arrays.equals(equalsData, other.equalsData))
			return false;
		return true;
	}
}
