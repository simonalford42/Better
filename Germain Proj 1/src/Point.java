import java.util.Arrays;

public class Point {
	public int id;
	private static int idCounter = 0;
	public int[] attrs;
	public int freq;
	
	public Point(int[] attrs) {
		this.attrs = attrs;
		this.id = idCounter;
		idCounter++;
		this.freq = 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(attrs);
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
		Point other = (Point) obj;
		if (!Arrays.equals(attrs, other.attrs))
			return false;
		return true;
	}
	
}
