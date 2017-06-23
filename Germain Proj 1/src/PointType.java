import java.util.Arrays;

public class PointType {
	public static String labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz";
	public final int id;
	public static int idCounter = 0;
	public final String label;
	public final String info;
	public final String[] data; //has all the data for ease of use
	
	public PointType(String info, String[] data) {
		this.data = data;
		this.info = info;
		this.id = idCounter;
		idCounter++;
		this.label = labels.substring(id, id+1);
	}
	
	public double distance(PointType other) {
		int size = this.data.length;
		double distance = 0;

		//for each attribute
		for (int i = 0; i < size; i++) {
			if (Simpler.weights[i] == 0) {
				continue;
			}
			double attrDist;
			switch(Simpler.attrTypes[i]) {
			case 0:	
				if (this.data[i].equals(other.data[i])) {
					attrDist = 0;
				} else {
					attrDist = Simpler.weights[i];
				}
				break;
			case 1:
				boolean b1 = this.data[i].equals("<none>") || this.data[i].equals("<N/A>");
				boolean b2 = this.data[i].equals("<none>") || other.data[i].equals("<N/A>");
				
				//if both SimplePoints have numerical values here
				if (!b1 && !b2) {
					double d1 = Double.valueOf(this.data[i]);
					double d2 = Double.valueOf(other.data[i]);
					attrDist = Simpler.weights[i]*(1.0/Simpler.rangeNumVals[i])*Math.abs(d1 - d2);
				} else if (b1 && b2) {
					attrDist = 0;
				} else {
					attrDist = Simpler.weights[i];
				}
				break;
				
			default:
				attrDist = 0;
				throw new java.lang.RuntimeException("this not right");
			}
			distance += attrDist;
		}
		return distance;
	}
	
	public String toString() {
		return label;
	}
	
	public static String getLabel(int i) {
		return labels.substring(i, i+1);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		PointType other = (PointType) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	
}
