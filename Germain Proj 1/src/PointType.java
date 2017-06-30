import java.util.Arrays;

public class PointType {
	public final int id;
	public static int idCounter = 0;
	public final String label;
	public final String[] data; //has all the data for ease of use
	public final String info;
	
	public PointType(String[] data, String info) {
		this.data = data;
		this.id = idCounter;
		idCounter++;
		this.label = info.substring(0, 1);
		this.info = info;
	}
	
	public String toString() {
		return Arrays.toString(data);
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
