import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

public class Subpoint implements Comparable<Subpoint> {
	public static int idCounter = 0;
	
	public int id;
	public int equalsId;
	
	public Date timestamp;
	public String user;
	public int[] attrs;
	
	public Subpoint(Date timestamp, String user, int[] attrs) {
		this.id = idCounter;
		idCounter++;

		this.timestamp = timestamp;
		this.user = user;
		this.attrs = attrs;
	}
	
	public int compareTo(Subpoint other) {
		return timestamp.compareTo(other.timestamp);
	}
	
	public String toString() {
		return user + " " + Arrays.toString(attrs);
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
		
		Subpoint other = (Subpoint) obj;
		if (!Arrays.equals(attrs, other.attrs))
			return false;
		
		return true;
	}
	
}
