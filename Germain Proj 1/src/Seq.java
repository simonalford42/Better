import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;

public class Seq {
	List<PointType> data;
	int length;
	
	public Seq(List<PointType> data) {
		this.data = data;
		this.length = data.size();
	}
	
	public double distance(Seq other) {
		if(this == other)
			return 0;
		
		//this way nothing is 0 from anything but itself. Changing this number should not affect a ton.s
		double distance = 1;
		
		int minSize = Math.min(this.length, other.length);
		int maxSize = Math.max(this.length, other.length);
		
		for(int i = 0; i < minSize; i++) {
			PointType th = this.data.get(i);
			PointType ot = other.data.get(i);
			double subdist = Simpler.distance(th, ot);
			//System.out.print(subdist + ", ");
			distance += subdist;
		}
		
		//System.out.println(")");
		double sizePenalty = (maxSize - minSize)*Simpler.maxDist;
		//System.out.println("sp " + sizePenalty);
		distance += sizePenalty;
		distance /= (double)maxSize;
		
		return distance;
	}
	
	public String toString() {
		String s = "";
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
		SimpleSeq other = (SimpleSeq) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}
}
