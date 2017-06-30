import java.util.Arrays;
import java.util.Scanner;

public class Subpoint implements Comparable<Subpoint> {
	public static int idCounter = -1; //starts at -1 because one gets used up on the attribute labels
	public int id;
	public int equalsId;
	public String label;
	public String[] data;
	
	public Subpoint(String[] attrList) {
		this.id = idCounter;
		idCounter++;
		this.data = attrList;
	}
	
	public static double distance(String[] attrList1, String[] attrList2, double[] weights, int[] attrTypes) {
		//System.out.println();
		int size = attrList1.length;
		double distance = 0;

		//for each attribute
		for (int i = 0; i < size; i++) {
			if (weights[i] == 0) {
				continue;
			}
			double attrDist;
			switch(attrTypes[i]) {
			case 0:	
				if (attrList1[i].equals(attrList2[i])) {
					attrDist = 0;
				} else {
					attrDist = weights[i];
				}
				break;
			case 1:
				// Because FreshK sets rangeNumVals according to all of the points, 
				// but simpler sets rangeNumVals only according to the rep points.
				//so this could make the distances different = clusters different.
				System.err.println("Should use numeric data for distances");
				boolean b1 = attrList1[i].equals("<none>") || attrList1[i].equals("<N/A>");
				boolean b2 = attrList2[i].equals("<none>") || attrList2[i].equals("<N/A>");
				
				//if both subpoints have numerical values here
				if (!b1 && !b2) {
					double d1 = Double.valueOf(attrList1[i]);
					double d2 = Double.valueOf(attrList2[i]);
					attrDist = 0;//weights[i]*(1.0/rangeNumVals[i])*Math.abs(d1 - d2);
				} else {
					attrDist = 0;
				}
				break;
			case 2:
				String s1 = attrList1[i];
				String s2 = attrList2[i];
				attrDist = weights[i]*FreshK.stringDist(s1, s2); 
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
			switch(FreshK.attrTypes[i]) {
			case 0:	
				if (this.data[i].equals(other.data[i])) {
					attrDist = 0;
				} else {
					attrDist = FreshK.weights[i];
				}
				break;
			case 1:
				// Because FreshK sets rangeNumVals according to all of the points, 
				// but simpler sets rangeNumVals only according to the rep points.
				//so this could make the distances different = clusters different.
				System.err.println("Should use numeric data for distances");
				boolean b1 = this.data[i].equals("<none>") || this.data[i].equals("<N/A>");
				boolean b2 = other.data[i].equals("<none>") || other.data[i].equals("<N/A>");
				
				//if both subpoints have numerical values here
				if (!b1 && !b2) {
					double d1 = Double.valueOf(this.data[i]);
					double d2 = Double.valueOf(other.data[i]);
					attrDist = FreshK.weights[i]*(1.0/FreshK.rangeNumVals[i])*Math.abs(d1 - d2);
				} else {
					attrDist = 0;
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
		return Arrays.toString(data);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for(int i = 0; i < data.length; i++) {
			if (FreshK.weights[i] != 0) {
				result = prime * result + data[i].hashCode();
			}
		}
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
		for(int i = 0; i < data.length; i++) {
			if (FreshK.weights[i] != 0) {
				if (!this.data[i].equals(other.data[i])) {
					return false;
				}
			}
		}
		return true;
	}
	
}
