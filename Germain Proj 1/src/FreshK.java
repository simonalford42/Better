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
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Class so that it works on any data thing instead of having
 * a bunch of different classes for each type.
 * @author alfordsimon
 *
 */
public class FreshK {
	public static String[] attributeLabels;
	public static int[] compareTypes;
	public static double[] weights;
	public static ArrayList<Sequence> data;
	public static double[] rangeNumVals;
	public static int timeStampIndex;
	public static DateParser dateParser;
	public static int numAttrs;
	public static double maxDist;
	public static ArrayList<ArrayList<Sequence>> finalClusters;
	public static Sequence[] finalMeans;
	public static int equalsLength;
	public static ArrayList<Subpoint> subpoints;
	public static ArrayList<ArrayList<Subpoint>> finalSubClusters;
	public static Subpoint[] finalSubMeans;
	public static Map<Tuple<String, String>, Double> stringDistLookup = new HashMap<>();
	
	public static DistanceMetric<Sequence> defaultDist = new DistanceMetric<Sequence>() {
		public double distance(Sequence s1, Sequence s2) {
			return s1.distance(s2);
		}
	};
	
	public static DistanceMetric<Sequence> levDist = new DistanceMetric<Sequence>() {
		public double distance(Sequence s1, Sequence s2) {
			return Levenshtein.hardLevinshtein(s1, s2, 1);
		}
	};
	
	public static DistanceMetric<Sequence> softLevDist = new DistanceMetric<Sequence>() {
		public double distance(Sequence s1, Sequence s2) {
			return Levenshtein.softLevi(s1, s2, maxDist);
		}
	};
	
	public FreshK(String dataFilePath, double[] weights, int[] compareTypes, int sequenceIDIndex,
			int timeStampIndex, DateParser dp) {
		System.out.println("Loading");
		long time = System.currentTimeMillis();
		
		FreshK.timeStampIndex = timeStampIndex;
		FreshK.numAttrs = compareTypes.length;
		dateParser = dp;
		
		FreshK.weights = weights;
		maxDist = 0;
		equalsLength = 0;
		for (double w: weights) {
			if (w != 0)
				equalsLength++;
			maxDist += w;
		}
		
		FreshK.compareTypes = compareTypes;
		rangeNumVals = new double[compareTypes.length];
		double[] largestNumVals = new double[compareTypes.length];
		double[] smallestNumVals = new double[compareTypes.length];
		
		for(int i = 0; i < compareTypes.length; i++) {
			largestNumVals[i] = Double.MIN_VALUE;
			smallestNumVals[i] = Double.MAX_VALUE;
		}
		
		ArrayList<Integer> numIndeces = new ArrayList<>();
		for(int i = 0; i < compareTypes.length; i++) {
			int type = compareTypes[i];
			if (type == 1) {
				numIndeces.add(i);
			}
		}
		
		data = new ArrayList<>();
		Map<String, ArrayList<Subpoint>> sacs = new HashMap<>();
		subpoints = new ArrayList<>();
			
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(new File(dataFilePath)));
			String line = br.readLine();
			attributeLabels = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
			
			while ((line = br.readLine()) != null) {
				if (line.charAt(line.length() - 1) != '\"') {
					String line2 = br.readLine();
					line += line2;
				}
				
				Subpoint sp = new Subpoint(line);
				subpoints.add(sp);
				
				String user = sp.data[sequenceIDIndex];
				if (!sacs.containsKey(user)) {
					sacs.put(user, new ArrayList<Subpoint>());
				}
				sacs.get(user).add(sp);
				
				for(int numIndex: numIndeces) {
					try {
						double d = Double.valueOf(sp.data[numIndex]);
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
		
		for(int numIndex: numIndeces) {
			rangeNumVals[numIndex] = largestNumVals[numIndex] - smallestNumVals[numIndex];
		}
		
		for(String key: sacs.keySet()) {
			ArrayList<Subpoint> cluster = sacs.get(key);
			data.add(new Sequence(cluster));
		}

		long time2 = System.currentTimeMillis();
		long secs = (time2 - time) / 1000;
		System.out.println("Loading done (" + secs + " seconds)");
	}
	
	public static double stringDist(String s1, String s2) {
		Tuple<String, String> t1 = new Tuple<>(s1, s2);
		if (stringDistLookup.containsKey((t1)))
			return stringDistLookup.get(t1);
		Tuple<String, String> t2 = new Tuple<>(s2, s1);

		double k = 0;
		for(int j = 0; j < Math.min(s1.length(), s2.length()); j++) {
			if (s1.charAt(j) == s2.charAt(j))
				k++;
		}
		double l = Math.max(s1.length(), s2.length());
		double dist = (l - k) / l;
		stringDistLookup.put(t1, dist);
		stringDistLookup.put(t2, dist);
		return dist;
	}
	
	public static Sequence[] kMeans(int k) {
		System.out.println("K-means");
		//initialize the k means to random FreshSequences
		Sequence[] means = initMeans(k);
		ArrayList<ArrayList<Sequence>> clusters = null;
		ArrayList<ArrayList<Sequence>> oldClusters;
		
		do { //repeat until convergence
			//System.out.println("New round");
			
			oldClusters = clusters;
			//k-means is not good with anything but the defaultDist currently.
			clusters = cluster(means, defaultDist);
			
			//update means by averaging its associates
			Sequence[] newMeans = new Sequence[k];
			for(int i = 0; i < k; i++) {
//				System.out.println("k" + (i+1));
				ArrayList<Sequence> cluster = clusters.get(i);
//				System.out.println("cluster = " + cluster.size());
				
				newMeans[i] = average(cluster);
			}
			
			means = newMeans;
//			System.out.println("Round over\n");
		} while (!clusters.equals(oldClusters));
		
		finalMeans =  means;
		finalClusters = clusters;
		return means;
	}
	
	private static Sequence[] initMeans(int k) {
		Sequence[] means = new Sequence[k];
		Random rand = new Random();
		ArrayList<Integer> used = new ArrayList<Integer>();
		int n = 0;
		for (int i = 0; i < k; i++) {
			do {
				n = rand.nextInt(data.size());
			} while (used.contains(n));
		
			means[i] = data.get(n);
			used.add(n);
		}
		
		return means;
	}
	
	public static Subpoint[] kSubMeans(int k) {
		Subpoint[] means = new Subpoint[k];
		int maxIters = 15;
		//initialize to random subpoints
		Random rand = new Random();
		ArrayList<Integer> used = new ArrayList<Integer>();
		int n = 0;
		for (int i = 0; i < k; i++) {
			do {
				n = rand.nextInt(subpoints.size());
			} while (used.contains(n));
		
			means[i] = subpoints.get(n);
			used.add(n);
		}
		
		ArrayList<ArrayList<Subpoint>> clusters = null;
		ArrayList<ArrayList<Subpoint>> oldClusters;
		int iter = 0;
		do {
			iter++;
			oldClusters = clusters;
			
			clusters = new ArrayList<ArrayList<Subpoint>>(k);
			for (int i = 0; i < k; i++) {
				clusters.add(new ArrayList<Subpoint>());
			}
			
			// associate each subpoint with its nearest mean
			for (Subpoint s: subpoints) {
				double minDistance = Integer.MAX_VALUE;
				int nearest = 0;
				for (int i = 0; i < k; i++) {
					Subpoint mean = means[i];
					double dist = s.distance(mean);
					
					if (dist < minDistance) {
						minDistance = dist;
						nearest = i;
					}
				}
				clusters.get(nearest).add(s);
			}
			
			int newK = k;
			for(int i = 0; i < newK; i++) {
				if (clusters.get(i).size() < 10) {
					clusters.remove(i);
					newK--;
					i--;
				}
			}
	
			k = newK;
			
			//update means by choosing member that minimizes distance
			Subpoint[] newMeans = new Subpoint[k];
			for(int i = 0; i < k; i++) {
				ArrayList<Subpoint> cluster = clusters.get(i);
				
				String[] attrList = new String[FreshK.numAttrs];
				//for each attribute
				for(int attr = 0; attr < numAttrs; attr++) {
					switch(compareTypes[attr]) {
					case 0:
						//categorical, so find the most common value and that is the value for the new mean
	
						//create frequency list for values
						HashMap<String, Integer> freqList = new HashMap<String, Integer>();
						for(Subpoint s: cluster) {
							String key = s.data[attr];
							Integer val = freqList.get(key);
							freqList.put(key, (val == null ? 1 : val + 1));
						}
						
						//find most common
						String mostCommon = "";
						int maxFreq = 0;
						for(String key: freqList.keySet()) {
							if (freqList.get(key) > maxFreq) {
								mostCommon = key;
								maxFreq = freqList.get(key);
							}
						}
						attrList[attr] = mostCommon;
						break;
						
					case 1:
						//numerical, so find the average value and use that.
						double total = 0;
						int count = 0;
						int other = 0;
						for(Subpoint s: cluster) {
							
							String val = s.data[attr];
							if (val.equals("<N/A>")) {
								other++;
								continue;
							}
							count++;
							total += Double.valueOf(val);
						}
						
						double avg = total / count;
						
						if (other > count) {
							attrList[attr] = "<N/A>";
						} else {
							attrList[attr] = Double.toString(avg);
						}
						break;
					case 2:
						//medoids approach
						String min = cluster.get(0).data[attr];
						double minDist = Double.MAX_VALUE;
						for(Subpoint s: cluster) {
							double dist = 0;
							String st1 = s.data[attr];
							for(Subpoint s2: cluster) {
								String st2 = s2.data[attr];
								for(int j = 0; j < Math.min(st1.length(), st2.length()); j++) {
									if (st1.charAt(j) != st2.charAt(j))
										dist++;
								}
							}
							if (dist < minDist) {
								minDist = dist;
								min = st1;
							}
						}
						
						attrList[attr] = min;
						break;
					default: 
						throw new java.lang.RuntimeException("this not right");
					}
				}//end for each attribute

				System.out.println(cluster.size() + " " + Arrays.toString(attrList));
				newMeans[i] = new Subpoint(attrList);
			}
			
			means = newMeans;
			
			System.out.println("Round over\n");
		} while (iter <= maxIters && !clusters.equals(oldClusters));
			
		finalSubMeans = means;
		finalSubClusters = clusters;
		return means;
	}

	
	public static Sequence[] kMedoids(int k, DistanceMetric<Sequence> dm) {
		System.out.println("K-medoids");
		//initialize the k means to random FreshSequences
		Sequence[] means = initMeans(k);
		ArrayList<ArrayList<Sequence>> clusters = null;
		ArrayList<ArrayList<Sequence>> oldClusters;
		
		do { //repeat until convergence
			//System.out.println("New round");
			
			oldClusters = clusters;
			clusters = cluster(means, dm);
			
			//update means by choosing member that minimizes distance
			Sequence[] newMeans = new Sequence[k];
			for(int i = 0; i < k; i++) {
			//	System.out.println("k" + (i+1));
				ArrayList<Sequence> cluster = clusters.get(i);
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
	
	private static ArrayList<ArrayList<Sequence>> cluster(Sequence[] means, DistanceMetric<Sequence> dm) {
		int k = means.length;
		ArrayList<ArrayList<Sequence>> clusters = new ArrayList<ArrayList<Sequence>>(k);
		for (int i = 0; i < k; i++) {
			clusters.add(new ArrayList<Sequence>());
		}
		
		// associate each Sequence with its nearest mean
		for (Sequence s: data) {
			double minDistance = Integer.MAX_VALUE;
			int nearest = 0;
			for (int i = 0; i < k; i++) {
				Sequence mean = means[i];
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
	
	private static Sequence medoid(ArrayList<Sequence> cluster, DistanceMetric<Sequence> dm) {
		double[] totalDistances = new double[cluster.size()];
		double minDist = Integer.MAX_VALUE;
		Sequence min = cluster.get(0);
		
		for(int j = 0; j < cluster.size(); j++) {
			double dist = 0;
			Sequence fs = cluster.get(j);
			
			for(Sequence fs2: cluster) {
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
	
	private static Sequence average(ArrayList<Sequence> sequences) {
		//Make new Sequence average length
		int l = 0;
		for(Sequence s: sequences) {
			l += s.data.size();
		}
		l /= sequences.size(); //average length
		//System.out.println("l " + l);
		
		ArrayList<Subpoint> newSeqPoints = new ArrayList<Subpoint>(l);
		
		//for each Subpoint
		for(int j = 0; j < l; j++) {
			String[] attrList = new String[FreshK.numAttrs];
			//for each attribute
			for(int attr = 0; attr < numAttrs; attr++) {
				switch(compareTypes[attr]) {
				case 0:
					//categorical, so find the most common value and that is the value for the new mean

					//create frequency list for values
					HashMap<String, Integer> freqList = new HashMap<String, Integer>();
					for(Sequence s: sequences) {
						
						//Only averaging FreshSequences >= the average size for the farther out ones. Is this good?
						if (s.length <= j)
							continue;
						
						String key = s.data.get(j).data[attr];
						Integer val = freqList.get(key);
						freqList.put(key, (val == null ? 1 : val + 1));
					}
					
					//find most common
					String mostCommon = "";
					int maxFreq = 0;
					for(String key: freqList.keySet()) {
						if (freqList.get(key) > maxFreq) {
							mostCommon = key;
							maxFreq = freqList.get(key);
						}
					}
					attrList[attr] = mostCommon;
					break;
					
				case 1:
					//numerical, so find the average value and use that.
					double total = 0;
					int count = 0;
					int other = 0;
					for(Sequence s: sequences) {
						
						if (s.length <= j) 
							continue;

						String val = s.data.get(j).data[attr];
						if (val.equals("<N/A>")) {
							other++;
							continue;
						}
						count++;
						total += Double.valueOf(val);
					}
					
					double avg = total / count;
					
					if (other > count) {
						attrList[attr] = "<N/A>";
					} else {
						attrList[attr] = Double.toString(avg);
					}
					break;
				default: 
					System.err.println("Not good");
					System.exit(0);
					break;
				}
			}//end for each attribute

			Subpoint sub = new Subpoint(attrList);
			newSeqPoints.add(sub);
		}//end for each Subpoint
		
		Sequence avg = new Sequence(newSeqPoints);
		return avg;
	}
	
	public static void pause() {
		System.out.println("(Paused)");
		new Scanner(System.in).nextLine();
	}
	
	public static void print(Sequence seq) {
		System.out.println("id " + seq.id + " length " + seq.length);
		//for(Subpoint sub: seq.data) {
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
		for(Sequence s: data) {
			if (s.distance(s) != 0) {
				System.out.println(s.toString());
				new Scanner(System.in).nextLine();
			}
		}
	}
	
	public static void testSymmetricity() {
		for(Sequence s1: data) {
			for(Sequence s2: data) {
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
		for (Sequence s1: data) {
			for (Sequence s2: data) {
				for (Sequence s3: data) {
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
	
	public static FreshK makeNewData(boolean isSmallVersion) {
		double[] weights = {0, 		0, 		  0,  			1,  		1,	 	1, 		1, 		 1, 		1, 			1, 				0};
		//		UXT_START_TIME	IPV4_ADDR	USERNAME	URL_PATH	CATEGORY	NAME	NAME2	UXT_METHOD	UXT_STATUS	UXT_DURATION	<N/A>'
		// 0 is for categorical data, 1 is for numerical, 2 is for string matching
		int[] compareTypes = {0,0,0,		0,0,0, 	2,0,0, 	1, 0};	
		int timeStampIndex = 0;
		int idIndex = 2;
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
		
		String filePath = "/Users/alfordsimon/Desktop/Germain data/First_Big.csv";
		if (isSmallVersion) {
			filePath = "First_Small.csv";
		}
		return new FreshK(filePath, weights, compareTypes, idIndex, timeStampIndex, dp);
		
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
	
	public static ArrayList<Sequence> partition(Sequence s) {
		ArrayList<Sequence> bestPartitions = new ArrayList<Sequence>();
		bestPartitions.add(s);
		double minTotalDist = Double.MAX_VALUE;
		for(int i = 3; i < Math.min(s.length, 20); i++) {
			Sequence first = s.subsequence(0, i);
			ArrayList<Sequence> partitions = new ArrayList<Sequence>();
			double totalDist = 0;
			int i2 = i + 1;
			while (i2 < s.length) {
				double minDist = Double.MAX_VALUE;
				int minAdd = 0;
				Sequence next;
				for (int add = 1; add < first.length*2; add++) {
					if (i2 + add >= s.length + 1) {
						break;
					}
					next = s.subsequence(i2, i2 + add);
					double dist = softLevDist.distance(first, next);
					if (dist < minDist) {
						minDist = dist;
						minAdd = add;
					}
				}
				partitions.add(s.subsequence(i2, i2 + minAdd));
				totalDist += minDist;
				i2 = i2 + minAdd;
			}
			
			if (totalDist < minTotalDist) {
				bestPartitions = partitions;
				minTotalDist = totalDist;
			}
		}
		
		return bestPartitions;
	}
	
	public static void kSubMeansStuff(int k, boolean save) {
		kSubMeans(k);
		k = finalSubClusters.size();
		System.out.println(k);
		int[] indeces = new int[k];
		int[] sizes = new int[k];
		int[][] array = {indeces, sizes};
		int sortRow = 1;
		
		for(int i = 0; i < k; i++) {
			sizes[i] = finalSubClusters.get(i).size();
			indeces[i] = i;
		}
		
		for(int i = 0; i < array[0].length - 1; i++)
		{
			for(int j = i+1; j < array[0].length; j++)
			{
				if(array[sortRow][i] > array[sortRow][j])
				{
					int temp = array[0][i];
					array[0][i] = array[0][j];
					array[0][j] = temp;
					temp = array[1][i];
					array[1][i] = array[1][j];
					array[1][j] = temp;
				}
			}
		}	
		
		indeces = array[0];
		sizes = array[1];
		
		for(int j = 0; j < k; j++) {
			int i = indeces[j];
			double avgDist = 0;
			for(Subpoint s: finalSubClusters.get(i)) {
				avgDist += finalSubMeans[i].distance(s);
			}
			avgDist /= finalSubClusters.get(i).size();
			
			Map<Subpoint, Integer> freqMap = freqMap(finalSubClusters.get(i));
			System.out.println("Unique ID size = " + freqMap.size());
			System.out.println(" Cluster  size = " + finalSubClusters.get(i).size()
					+ " avg dist = " + avgDist);
			System.out.println("mean = " + finalSubMeans[i].toString());
			System.out.println(getStats(finalSubClusters.get(i)));
		}
		
		if (!save)
			return;
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("K-means_" + k + "points.txt"));
			for(int j = 0; j < k; j++) {
				int i = indeces[j];
				
				String line = "";
				for(Subpoint s: finalSubClusters.get(i)) {
					line += s.id + ",";
				}
				line = line.substring(0, line.length() - 1) + "\n";
				writer.write(line);
				
				double avgDist = 0;
				for(Subpoint s: finalSubClusters.get(i)) {
					avgDist += finalSubMeans[i].distance(s);
				}
				avgDist /= finalSubClusters.get(i).size();
				
				Map<Subpoint, Integer> freqMap = freqMap(finalSubClusters.get(i));
				writer.write("Unique ID size = " + freqMap.size() + "\n");
				writer.write(" Cluster  size = " + finalSubClusters.get(i).size()
						+ " avg dist = " + avgDist + "\n");
				writer.write("mean = " + finalSubMeans[i].toString() + "\n");
				writer.write(getStats(finalSubClusters.get(i)) + "\n");
				writer.write("end info\n");
			}
			writer.write("done");
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String getStats(ArrayList<Subpoint> cluster) {
		ArrayList<Map<String, Integer>> stats = new ArrayList<>();
		String stuff = "";
		
		for(int i = 0; i < cluster.get(0).data.length; i++) {
			if (FreshK.weights[i] == 0) {
				continue;
			}
			stuff += i + ": ";
			ArrayList<String> items = new ArrayList<>(); 
			for(Subpoint s: cluster) {
				items.add(s.data[i]);
			}
			
			Map<String, Integer> freqMap = sortByValue(freqMap(items));
			for(int j = 0; j < Math.min(3, freqMap.size()); j++) {
				String key = freqMap.keySet().toArray(new String[freqMap.size()])[freqMap.size() - 1 - j];
				stuff += key + " " + (100*freqMap.get(key) / cluster.size()) + ", ";
			}
			stuff += "\n";
			stats.add(freqMap);
			
		}
		return stuff.substring(0, stuff.length() - 2);

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
	
	public static void main(String[] args) {
		makeNewData(false);
	}
}
