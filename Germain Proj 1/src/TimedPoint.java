import java.util.Date;

public class TimedPoint implements Comparable<TimedPoint>{
	public Date timestamp;
	public Point p;
	
	public TimedPoint(Date timestamp, Point p) {
		this.timestamp = timestamp;
		this.p = p;
	}

	@Override
	public int compareTo(TimedPoint o) {
		return this.timestamp.compareTo(o.timestamp);
	}
	
	
}
