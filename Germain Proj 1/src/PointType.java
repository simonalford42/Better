
public class PointType {
	public static String labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz";
	public final int id;
	public static int idCounter = 0;
	public final String label;
	public final String[] data; //has all the data for ease of use
	
	public PointType(String[] data) {
		this.data = data;
		this.id = idCounter;
		idCounter++;
		this.label = labels.substring(id, id+1);
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
