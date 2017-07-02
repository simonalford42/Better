import java.util.List;

public class Seq {
	public List<PointType> data;
	public int length;
	public String user;
	
	public Seq(List<PointType> data, String user) {
		this.data = data;
		this.length = data.size();
		this.user = user;
	}
	
	public String toString() {
		String s = user + " ";
		for(PointType i: data) {
			s += i.label;
		}
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
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
		Seq other = (Seq) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}
}
