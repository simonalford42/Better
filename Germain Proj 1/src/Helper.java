import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Helper {
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
	    return map.entrySet()
	              .stream()
	              .sorted(Map.Entry.comparingByValue(/*Collections.reverseOrder()*/))
	              .collect(Collectors.toMap(
	                Map.Entry::getKey, 
	                Map.Entry::getValue, 
	                (e1, e2) -> e1, 
	                LinkedHashMap::new
	              ));
	}
	
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByKey(Map<K, V> map) {
	    return map.entrySet()
	              .stream()
	              .sorted(Map.Entry.comparingByValue(/*Collections.reverseOrder()*/))
	              .collect(Collectors.toMap(
	                Map.Entry::getKey, 
	                Map.Entry::getValue, 
	                (e1, e2) -> e2, 
	                LinkedHashMap::new
	              ));
	}
	
	public static double stdev(ArrayList<Double> vals, double mean) {
		double n = vals.size();
		double std = 0;
		for(double val: vals) {
			std += (mean - val)*(mean - val);
		}
		std /= n;
		std = Math.sqrt(std);
		return std;
	}
	
	public static <T> Map<T, Integer> freqMap(Collection<T> items) {
		Map<T, Integer> freqMap = new LinkedHashMap<T, Integer>();
		
		for(T t: items) {
			Integer val = freqMap.get(t);
			freqMap.put(t, (val == null ? 1 : val + 1));
		}
		
		return freqMap;
	}
	
	public static void pause() {
		System.out.println("(Paused)");
		String s = new Scanner(System.in).nextLine();
		if (s.contains("N") || s.contains("n"))
			System.exit(0);
	}
}
