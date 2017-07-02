import java.util.Arrays;

public class PointType {
	public int id;
	private static int idCounter = 0;
	public String[] attrs;
	public String label;
	public String info;
	
	public PointType(String[] attrs) {
		this.attrs= attrs;
		this.id = idCounter;
		idCounter++;
	}
	
	public PointType(String[] attrs, String info) {
		this.id = idCounter;
		idCounter++;

		this.attrs = attrs;
		this.label = info.substring(0, 1);
		this.info = info;
	}
	
	public String toString() {
		return Arrays.toString(attrs);
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
