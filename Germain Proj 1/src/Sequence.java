import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class Sequence implements Comparable<Sequence>{
	private static int idCounter = 0;
	public int id;
	
	List<Subpoint> data;
	int length;
	String user;
	
	public Sequence(List<Subpoint> data) {
		this.data = data;
		this.length = data.size();
		id = idCounter;
		idCounter++;
	}
	
	public Sequence(String user) {
		this.user = user;
		this.data = new ArrayList<Subpoint>();
		this.length = 0;
		this.id = idCounter;
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
		return new Sequence(data.subList(fromIndex, toIndex));
	}
	
	public void add(Subpoint s) {
		data.add(s);
		length++;
	}
	
	public boolean contains(Subpoint s) {
		return data.contains(s);
	}
	
	public double distance(Sequence other) {
		if(this == other)
			return 0;
		
		//this way nothing is 0 from anything but itself. Changing this number should not affect a ton.s
		double distance = 1;
		
		int minSize = Math.min(this.length, other.length);
		int maxSize = Math.max(this.length, other.length);
		
		for(int i = 0; i < minSize; i++) {
			Subpoint th = this.data.get(i);
			Subpoint ot = other.data.get(i);
			double subdist = th.distance(ot);
			//System.out.print(subdist + ", ");
			distance += subdist;
		}
		
		//System.out.println(")");
		double sizePenalty = (maxSize - minSize)*FreshK.maxDist;
		//System.out.println("sp " + sizePenalty);
		distance += sizePenalty;
		distance /= (double)maxSize;
		
		return distance;
	}
	
	public String toString() {
		return "id" + id + " l" + length + " " + data.toString();
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
		Sequence other = (Sequence) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}

	/**
	 * Just compares their id's. 
	 */
	@Override
	public int compareTo(Sequence o) {
		return Integer.compare(this.id, o.id);
	}
}
