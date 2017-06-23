import java.util.Arrays;

public class SimplePoint implements Comparable<SimplePoint> {
	public String timestamp;
	public String user;
	public PointType point;
	
	public SimplePoint(String timestamp, String user, int pointID) {
		this.timestamp = timestamp;
		this.user = user;
		point = Simpler.pointTypes[pointID];
	}
	
	/**
	 * for sorting clusters of SimplePoints into time-ordered sequences
	 * timestamp is SimplePoint.data[0]. converts it to a Date then compares time ordering
	 */
	public int compareTo(SimplePoint other) {
		return Simpler.dateParser.makeDate(this.timestamp).compareTo(Simpler.dateParser.makeDate(other.timestamp));
	}
	
	public String toString() {
		return timestamp + " " + user + " " + point.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((point == null) ? 0 : point.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		SimplePoint other = (SimplePoint) obj;
		if (point == null) {
			if (other.point != null)
				return false;
		} else if (!point.equals(other.point))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

	
}
