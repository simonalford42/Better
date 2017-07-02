import java.util.List;

public class Sequence {
	public int id;
	private static int idCounter = 0;
	
	List<Point> points;
	int length;
	String user;
	
	public Sequence(List<Point> points) {
		this.points = points;
		this.length = points.size();
		id = idCounter;
		idCounter++;
	}
	
	/**
	 * Returns a view of the portion of this list between the specified fromIndex, inclusive, and toIndex, exclusive.
	 * (If fromIndex and toIndex are equal, the returned list is empty.) 
	 * @param start
	 * @param end
	 * @return
	 */
	public Sequence subsequence(int fromIndex, int toIndex) {
		return new Sequence(points.subList(fromIndex, toIndex));
	}
	
	public void add(Point p) {
		points.add(p);
		length++;
	}
	
	public boolean contains(Point p) {
		return points.contains(p);
	}
	
	public String toString() {
		return "id" + id + " l" + length + " " + points.toString();
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((points == null) ? 0 : points.hashCode());
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
		Sequence other = (Sequence) obj;
		if (points == null) {
			if (other.points != null)
				return false;
		} else if (!points.equals(other.points))
			return false;
		return true;
	}
}
