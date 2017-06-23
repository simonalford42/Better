import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Simpler {
	public static ArrayList<SimpleSeq> sequences;
	public static double[][] distanceLookup;
	public static PointType[] pointTypes;
	public static ArrayList<SimplePoint> points;
	
	public static String[] attributeLabels;
	public static int[] attrTypes;
	public static double[] weights;
	public static double[] rangeNumVals;
	public static int numAttrs;
	public static double maxDist;
	public static DateParser dateParser;
	public static int timeStampIndex;
	
	public static ArrayList<ArrayList<SimpleSeq>> finalClusters;
	public static SimpleSeq[] finalMeans;
	public static ArrayList<ArrayList<PointType>> finalSubClusters;
	public static Integer[] finalSubMeans;
	
	public static DistanceMetric<SimpleSeq> defaultDist = new DistanceMetric<SimpleSeq>() {
		public double distance(SimpleSeq s1, SimpleSeq s2) {
			return s1.distance(s2);
		}
	};
	
	public static DistanceMetric<SimpleSeq> levDist = new DistanceMetric<SimpleSeq>() {
		public double distance(SimpleSeq s1, SimpleSeq s2) {
			return Levenshtein.hardLevinshtein(s1, s2, 1);
		}
	};
	
	public static DistanceMetric<SimpleSeq> softLevDist = new DistanceMetric<SimpleSeq>() {
		public double distance(SimpleSeq s1, SimpleSeq s2) {
			return Levenshtein.softLevi(s1, s2, maxDist);
		}
	};
	
	/**
	 * @param dataFilePath -string to data file
	 * @param keyFilePath - string to file turning points into pointtypes
	 * @param weights - only for the equals data section
	 * @param attrTypes - 3 = timestamp, 4 = user, 0 = categorical, 1 = numerical, 2 = string matching  
	 * @param dp - converts timestamp string into date
	 */
	public Simpler(String dataFilePath, String keyFilePath, double[] weights, int[] attrTypes,
			DateParser dp) {
		
		System.out.println("Loading");
		long time = System.currentTimeMillis();
		
		Simpler.numAttrs = attrTypes.length;
		dateParser = dp;
		
		Simpler.weights = weights;
		maxDist = Arrays.stream(weights).sum();
		Simpler.attrTypes = attrTypes;

		rangeNumVals = new double[numAttrs];
		double[] largestNumVals = new double[numAttrs];
		double[] smallestNumVals = new double[numAttrs];
		
		
		for(int i = 0; i < numAttrs; i++) {
			largestNumVals[i] = Double.MIN_VALUE;
			smallestNumVals[i] = Double.MAX_VALUE;
		}
		
		int userIndex = -1;
		ArrayList<Integer> numIndeces = new ArrayList<>();
		for(int i = 0; i < numAttrs; i++) {
			int type = attrTypes[i];
			if (type == 1) {
				numIndeces.add(i);
			} else if (type == 3) {
				timeStampIndex = i;
			} else if (type == 4) {
				userIndex = i;
			}
		}
		
		sequences = new ArrayList<>();
		Map<String, ArrayList<SimplePoint>> sacs = new HashMap<>();
		Map<Integer, List<Integer>> idMap = new HashMap<>();
		Map<Integer, String> infoMap = new HashMap<>();
		Map<Integer, Integer> backwardsMap = new HashMap<>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(keyFilePath)));
			String line;
			int id = -1;
			while (!(line = br.readLine()).equals("done")) {
				String info = "";
				id++;
				idMap.put(id, Arrays.stream(line.split(","))
						.mapToInt(Integer::parseInt).boxed().collect(Collectors.toList()));
				for(int associate: idMap.get(id)) {
					backwardsMap.put(associate, id);
				}
				while (!(line = br.readLine()).equals("end info")) {
					info += line + "\n";
				}
				infoMap.put(id, info);
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		int numPoints = idMap.size();
		pointTypes = new PointType[numPoints];
		points = new ArrayList<>();
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File(dataFilePath)));
			String line = br.readLine();
			attributeLabels = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
			
			int i = -1;
			while ((line = br.readLine()) != null) {
				i++;
				if (line.charAt(line.length() - 1) != '\"') {
					String line2 = br.readLine();
					line += line2;
				}
				
				int id = backwardsMap.get(i);
				String info = infoMap.get(id);
				String[] data;
				data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
				for(int j = 0; j < data.length; j++) {
					if (data[j].length() > 0 && data[j].charAt(0) == '"')
						data[j] = data[j].substring(1,  data[j].length() - 1);
				}
				
				if (pointTypes[id] == null)
					pointTypes[id] = new PointType(info, data);
				
				String user = data[userIndex];
				SimplePoint sp = new SimplePoint(data[timeStampIndex], user, id);
				points.add(sp);
				
				if (!sacs.containsKey(user))
					sacs.put(user, new ArrayList<SimplePoint>());
				
				sacs.get(user).add(sp);
				
				for(int numIndex: numIndeces) {
					try {
						double d = Double.valueOf(pointTypes[id].data[numIndex]);
						if (d > largestNumVals[numIndex]) {
							largestNumVals[numIndex] = d;
						} else if (d < smallestNumVals[numIndex]) {
							smallestNumVals[numIndex] = d;
						}
					} catch (NumberFormatException e) {
						//this is okay, it just means we got an N/A and don't count it
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println(line);
						e.printStackTrace();
					}
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Distance lookups");
		distanceLookup = new double[numPoints][numPoints];
		for(int i = 0; i < numPoints; i++) {
			for(int j = i + 1; j < numPoints; j++) {
				double dist = pointTypes[i].distance(pointTypes[j]);
				distanceLookup[i][j] = dist;
				distanceLookup[j][i] = dist;
			}
		}
		
		for(int numIndex: numIndeces) {
			rangeNumVals[numIndex] = largestNumVals[numIndex] - smallestNumVals[numIndex];
		}
		
		for(String user: sacs.keySet()) {
			ArrayList<SimplePoint> cluster = sacs.get(user);
			SimpleSeq s = new SimpleSeq(cluster, user);
			sequences.add(s);
		}

		long time2 = System.currentTimeMillis();
		long secs = (time2 - time) / 1000;
		System.out.println("Loading done (" + secs + " seconds)");
	}
	
	public static double distance(PointType p1, PointType p2) {
		return distanceLookup[p1.id][p2.id];
	}
	
	private static SimpleSeq[] initMeans(int k) {
		SimpleSeq[] means = new SimpleSeq[k];
		Random rand = new Random();
		ArrayList<Integer> used = new ArrayList<Integer>();
		int n = 0;
		for (int i = 0; i < k; i++) {
			do {
				n = rand.nextInt(sequences.size());
			} while (used.contains(n));
		
			means[i] = sequences.get(n);
			used.add(n);
		}
		
		return means;
	}
	
	public static SimpleSeq[] kMedoids(int k, DistanceMetric<SimpleSeq> dm) {
		System.out.println("K-medoids");
		//initialize the k means to random FreshSimpleSeqs
		SimpleSeq[] means = initMeans(k);
		ArrayList<ArrayList<SimpleSeq>> clusters = null;
		ArrayList<ArrayList<SimpleSeq>> oldClusters;
		
		do { //repeat until convergence
			//System.out.println("New round");
			
			oldClusters = clusters;
			clusters = cluster(means, dm);
			
			//update means by choosing member that minimizes distance
			SimpleSeq[] newMeans = new SimpleSeq[k];
			for(int i = 0; i < k; i++) {
			//	System.out.println("k" + (i+1));
				ArrayList<SimpleSeq> cluster = clusters.get(i);
		//		System.out.println("cluster = " + cluster.size());
				
				newMeans[i] = medoid(cluster, dm);
			}
			
			means = newMeans;
	//		System.out.println("Round over\n");
		} while (!clusters.equals(oldClusters));
			
		finalMeans = means;
		finalClusters = clusters;
		
		return means;
	}
	
	private static ArrayList<ArrayList<SimpleSeq>> cluster(SimpleSeq[] means, DistanceMetric<SimpleSeq> dm) {
		int k = means.length;
		ArrayList<ArrayList<SimpleSeq>> clusters = new ArrayList<ArrayList<SimpleSeq>>(k);
		for (int i = 0; i < k; i++) {
			clusters.add(new ArrayList<SimpleSeq>());
		}
		
		// associate each SimpleSeq with its nearest mean
		for (SimpleSeq s: sequences) {
			double minDistance = Integer.MAX_VALUE;
			int nearest = 0;
			for (int i = 0; i < k; i++) {
				SimpleSeq mean = means[i];
				double dist;
				if (mean.id == s.id) {
					dist = 0;
				} else {
					dist = dm.distance(s, mean);
				}
				if (dist < minDistance) {
					minDistance = dist;
					nearest = i;
				}
			}
			clusters.get(nearest).add(s);
		}
		
		return clusters;
	}
	
	private static SimpleSeq medoid(ArrayList<SimpleSeq> cluster, DistanceMetric<SimpleSeq> dm) {
		double[] totalDistances = new double[cluster.size()];
		double minDist = Integer.MAX_VALUE;
		SimpleSeq min = cluster.get(0);
		
		for(int j = 0; j < cluster.size(); j++) {
			double dist = 0;
			SimpleSeq fs = cluster.get(j);
			
			for(SimpleSeq fs2: cluster) {
				double d = dm.distance(fs, fs2);
				dist += d;
			}

			totalDistances[j] = dist;
			if (dist < minDist) {
				minDist = dist;
				min = fs;
			}
		}
		
		return min;
	}
	
	public static void pause() {
		System.out.println("(Paused)");
		new Scanner(System.in).nextLine();
	}
	
	public static void print(SimpleSeq seq) {
		System.out.println("id " + seq.id + " length " + seq.length);
		//for(SimplePoint sub: seq.data) {
			//System.out.println(sub.toString());
		//}
	}
	
	public static <T> Map<T, Integer> freqMap(Collection<T> items) {
		Map<T, Integer> freqMap = new LinkedHashMap<T, Integer>();
		
		for(T t: items) {
			Integer val = freqMap.get(t);
			freqMap.put(t, (val == null ? 1 : val + 1));
		}
		
		return freqMap;
	}
	
	public static void testReflexivity() {
		for(SimpleSeq s: sequences) {
			if (s.distance(s) != 0) {
				System.out.println(s.toString());
				new Scanner(System.in).nextLine();
			}
		}
	}
	
	public static void testSymmetricity() {
		for(SimpleSeq s1: sequences) {
			for(SimpleSeq s2: sequences) {
				double d1 = s1.distance(s2);
				double d2 = s2.distance(s1);
				if (d1 != d2) {
					System.out.println(s1.toString());
					System.out.println(s2.toString());
					System.out.println(d1 + " " + d2);
					new Scanner(System.in).nextLine();
				}
			}
		}
	}
	
	public static void testTriangleEquality() {
		//triangle inequality
		for (SimpleSeq s1: sequences) {
			for (SimpleSeq s2: sequences) {
				for (SimpleSeq s3: sequences) {
					//System.out.println("d12");
					double d12 = s1.distance(s2);
					//new Scanner(System.in).nextLine();
					//System.out.println("d23");
					double d23 = s2.distance(s3);
					//new Scanner(System.in).nextLine();
					//System.out.println("d13");
					double d13 = s1.distance(s3);
					double eps = Math.pow(10, -7);
					if (d12 + d23 < d13 - eps) {
						System.out.println(s1.toString());
						System.out.println(s2.toString());
						System.out.println(s3.toString());
						System.out.println(d12 + " " + d23 + " " + d13);
						new Scanner(System.in).nextLine();
					}
				}
			}
		}
	}
	
	public static void testDist() {
		testReflexivity();
		System.out.println("reflex good");
		testSymmetricity();
		System.out.println("symmet good");
		testTriangleEquality();
		System.out.println("tri good");

	}
	
//	public static FreshK makeSimonData() {
//		double[] weights = {0, 		0, 		  1,  1,	 1, 	1, 		 0, 		0, 		1, 		1};
//		//				tStamp, src_addr,   url, cat, subcat, subcat2, method, status, duration, dom-content
//		// 0 is for categorical data, 1 is for numerical
//		int[] compareTypes = {0, 	0,			0,	0, 	0, 		0, 		0, 			0, 		1, 		0};
//		DateParser dp = new DateParser() {
//			public Date makeDate(String dateString) {
//				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//				Date d = null;
//				try {
//					d = format.parse(dateString);
//				} catch (ParseException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//				return d;
//			}
//		};
//		
//		int timeStampIndex = 0;
//		int idIndex = 2;
//		return new FreshK("Simon-Data.csv", weights, compareTypes, idIndex, timeStampIndex, dp);
//	}
	
	public static Simpler makeNewData(int k) {
		double[] weights = {0, 		0, 		  0,  			1,  		1,	 	1, 		1, 		 1, 		1, 			1, 				0};
		//		UXT_START_TIME	IPV4_ADDR	USERNAME	URL_PATH	CATEGORY	NAME	NAME2	UXT_METHOD	UXT_STATUS	UXT_DURATION	<N/A>'
		// 0 is for categorical data, 1 is for numerical,
		int[] dataTypes = {3,0,4,		0,0,0, 	0,0,0, 	1, 0};	
		DateParser dp = new DateParser() {
			public Date makeDate(String s) {
				String apm = s.substring(s.length() - 3);
				s = s.substring(0, s.length() - 9);
				s = s + apm;
				SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy hh.mm.ss.SSS a");
				Date d = null;
				
				try {
					d = format.parse(s);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				return d;
			}
		};
		
		String filePath = "First_Small.csv";
		String keyFilePath = "K-means_" + k + "points.txt";
		return new Simpler(filePath, keyFilePath, weights, dataTypes, dp);
		
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
	
	public static String getStats(ArrayList<Integer> cluster) {
		ArrayList<Map<String, Integer>> stats = new ArrayList<>();
		String stuff = "";
		
		for(int i = 0; i < pointTypes[cluster.get(0)].data.length; i++) {
			if (Simpler.weights[i] == 0) {
				continue;
			}
			stuff += i + ": ";
			ArrayList<String> items = new ArrayList<>(); 
			for(Integer s: cluster) {
				items.add(pointTypes[s].data[i]);
			}
			
			Map<String, Integer> freqMap = sortByValue(freqMap(items));
			for(int j = 0; j < Math.min(3, freqMap.size()); j++) {
				String key = freqMap.keySet().toArray(new String[freqMap.size()])[freqMap.size() - 1 - j];
				stuff += key + " " + (100*freqMap.get(key) / cluster.size()) + ", ";
			}
			stuff += "\n";
			stats.add(freqMap);
			
		}
		return stuff;

	}
	
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
	
	public static void main(String[] args) {
		makeNewData(10);
		List<String> data = sequences.stream().map(e -> e.data.stream().map(f -> f.label).collect(Collectors.joining(""))).collect(Collectors.toList());
		String[] common = PatternFinder.findMostCommon(data, 5, 15);
		Arrays.stream(common).forEach(System.out::println);
	}
}