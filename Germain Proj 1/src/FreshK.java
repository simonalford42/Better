import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;

/**
 * Class so that it works on any data thing instead of having
 * a bunch of different classes for each type.
 * @author alfordsimon
 *
 */
public class FreshK {
	public static ArrayList<Sequence> data;
	public static List<Subpoint> subpoints;

	public static int[] attrTypes;
	public static double[] weights;
	public static double[] rangeNumVals;
	public static double maxDist;
	public static int numAttrs;
	public static DateParser dateParser;
	public static int timeStampIndex;
	public static int userIndex;
	public static int equalsLength;
	
	public static Sequence[] finalMeans;
	public static ArrayList<ArrayList<Sequence>> finalClusters;
	public static ArrayList<ArrayList<Subpoint>> finalSubClusters;
	public static Subpoint[] finalSubMeans;
	public static String labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz!@#$%^&*()-=_+{}[]|:;<,>.?/~`";

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
	
	public static void importData(String dataFilePath, double[] weights, int[] attrTypes, DateParser dp) {
		System.out.println("Loading");
		long time = System.currentTimeMillis();
		
		numAttrs = attrTypes.length;
		dateParser = dp;
		
		FreshK.weights = weights;
		maxDist = Arrays.stream(weights).sum();
		FreshK.attrTypes = attrTypes;

		rangeNumVals = new double[numAttrs];
		double[] largestNumVals = new double[numAttrs];
		double[] smallestNumVals = new double[numAttrs];
		
		
		for(int i = 0; i < numAttrs; i++) {
			largestNumVals[i] = Double.MIN_VALUE;
			smallestNumVals[i] = Double.MAX_VALUE;
		}
		
		userIndex = -1;
		equalsLength = 0;
		ArrayList<Integer> numIndeces = new ArrayList<>();
		for(int i = 0; i < numAttrs; i++) {
			if (weights[i] != 0) {
				equalsLength++;
			}
			
			int type = attrTypes[i];
			if (type == 1) {
				numIndeces.add(i);
			} else if (type == 3) {
				timeStampIndex = i;
			} else if (type == 4) {
				userIndex = i;
			}
		}
		
		data = new ArrayList<>();
		Map<String, ArrayList<Subpoint>> sacs = new HashMap<>();
		subpoints = new ArrayList<>();
		
		try {
			CSVReader reader = new CSVReader(new FileReader(dataFilePath));
			subpoints = reader.readAll().stream().skip(1).map(e -> new Subpoint(e)).collect(Collectors.toList());
			System.out.println("halfwayish");
			
			for(Subpoint sp: subpoints) {
				String user = sp.data[userIndex];
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
						System.out.println(Arrays.toString(sp.data));
						e.printStackTrace();
					}
				}
			}
			reader.close();
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
		double secs = (double)(time2 - time) / 1000.0;
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
		List<Subpoint> uniqueSubpoints = new ArrayList<>(Helper.freqMap(subpoints).keySet());
		
		Subpoint[] means = new Subpoint[k];
		int maxIters = 100;
		//initialize to random subpoints
		Random rand = new Random();
		ArrayList<Integer> used = new ArrayList<Integer>();
		int n = 0;
		for (int i = 0; i < k; i++) {
			do {
				n = rand.nextInt(uniqueSubpoints.size());
			} while (used.contains(n));
		
			means[i] = uniqueSubpoints.get(n);
			used.add(n);
		}
		
		ArrayList<ArrayList<Subpoint>> clusters = null;
		ArrayList<ArrayList<Subpoint>> oldClusters;
		int iter = 1;
		
		do {
			System.out.println("iter " + iter + " / " + maxIters);
			oldClusters = clusters;
			
			clusters = new ArrayList<ArrayList<Subpoint>>(k);
			for (int i = 0; i < k; i++) {
				clusters.add(new ArrayList<Subpoint>());
			}
			
			// associate each subpoint with its nearest mean
			for (Subpoint s: uniqueSubpoints) {
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
					switch(attrTypes[attr]) {
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
					case 3:
						attrList[attr] = "Chinese dentist time";
						break;
					case 4:
						attrList[attr] = "My fair lady";
						break;
					default: 
						throw new java.lang.RuntimeException("this not right");
					}
				}//end for each attribute

				System.out.println(cluster.size() + " " + Arrays.toString(attrList));
				newMeans[i] = new Subpoint(attrList);
			}
			
			means = newMeans;
			iter++;
			System.out.println("Round over\n");
			System.out.println(iter <= maxIters);
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
				switch(attrTypes[attr]) {
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
	
	public static void kSubMeansStuff(int k, String filePath) {
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
		
		try {
			BufferedWriter dataWriter = new BufferedWriter(new FileWriter(filePath + "K-means_" + k + "points_data.txt"));
			BufferedWriter infoWriter = new BufferedWriter(new FileWriter(filePath + "K-means_" + k + "points_info.txt"));
			for(int j = 0; j < k; j++) {
				int i = indeces[j];
				String arrString = "";
				for(String s: finalSubMeans[i].data) {
					arrString += s + ",";
				}
				arrString = arrString.substring(0, arrString.length() - 1);
				dataWriter.write(arrString + "\n");
				
				double avgDist = 0;
				for(Subpoint s: finalSubClusters.get(i)) {
					avgDist += finalSubMeans[i].distance(s);
				}
				avgDist /= finalSubClusters.get(i).size();
				
				Map<Subpoint, Integer> freqMap = Helper.freqMap(finalSubClusters.get(i));
				infoWriter.write(labels.charAt(i) + "\n");
				infoWriter.write("Unique ID size = " + freqMap.size() + "\n");
				infoWriter.write(" Cluster  size = " + finalSubClusters.get(i).size()
						+ " avg dist = " + avgDist + "\n");
				infoWriter.write("mean = " + finalSubMeans[i].toString() + "\n");
				infoWriter.write(getStats(finalSubClusters.get(i)) + "\n");
				infoWriter.write("end info\n");
			}
			infoWriter.close();
			dataWriter.close();
			System.out.println("Saved file " + "K-means_" + k + "points_data.txt");
			System.out.println("Saved file " + "K-means_" + k + "points_info.txt");

		} catch (IOException e) {
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
			
			Map<String, Integer> freqMap = Helper.sortByValue(Helper.freqMap(items));
			for(int j = 0; j < Math.min(10, freqMap.size()); j++) {
				String key = freqMap.keySet().toArray(new String[freqMap.size()])[freqMap.size() - 1 - j];
				DecimalFormat df = new DecimalFormat("#.##");
				stuff += key + " " + df.format((double)100*freqMap.get(key) / (double)cluster.size()) + ", ";
			}
			stuff += "\n";
			stats.add(freqMap);
			
		}
		return stuff.substring(0, stuff.length() - 2);
	}
	
	public static void makeFirstData(String fileName) {
		Dataset first = Dataset.FIRST;
		double[] weights = first.weights;
		int[] attrTypes = first.attrTypes;
		DateParser dp = first.dp;
		
		String filePath = "/Users/alfordsimon/Desktop/Germain Data/First/" + fileName;
		importData(filePath, weights, attrTypes, dp);
	}
	
	public static void makeJune28Data(String fileName) {
		Dataset june28 = Dataset.JUNE28;
		double[] weights = june28.weights;
		int[] attrTypes = june28.attrTypes;
		DateParser dp = june28.dp;
		
		String filePath = "/Users/alfordsimon/Desktop/Germain Data/June 28/" + fileName;
		importData(filePath, weights, attrTypes, dp);
	}
	
	public static int findMean(Subpoint s) {
		int i = -1;
		double minDist = Double.MAX_VALUE;
		for(int j = 0; j < finalSubMeans.length; j++) {
			Subpoint mean = finalSubMeans[j];
			double dist = s.distance(mean);
			if (dist < minDist) {
				minDist = dist;
				i = j;
			}
		}
		return i;
	}

	public static void combineJune28() {
		List<Triple<String, String, Long>> list = new ArrayList<>();
		List<String> lines = new ArrayList<>();
		//import it all
		Dataset june = Dataset.JUNE28;
		FreshK.attrTypes = june.attrTypes;
		FreshK.dateParser = june.dp;
		FreshK.weights = june.weights;
		FreshK.equalsLength = weights.length;
		
		try {
			for (int i = 19; i < 20; i++) {
				for (int j = 1; j < 3; j++) {
					System.out.println("hello");
					String fileName = "simon-201706" + i + "." + j + ".csv";
					String fullName = "/Users/alfordsimon/Desktop/Germain data/June 28/" + fileName;
					CSVReader reader = new CSVReader(new FileReader(fullName));
					list.addAll(reader.readAll().stream().skip(1).map(e -> new Subpoint(e)).map(e -> new Triple<String, String, Long>("",e.data[2], FreshK.dateParser.makeDate(e.data[0]).getTime())).collect(Collectors.toList()));
					BufferedReader br = new BufferedReader(new FileReader(fullName));
					String line;
					while((line = br.readLine()) != null) {
						lines.add(line);
					}
					br.close();
					reader.close();
					System.out.println("good");
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		System.out.println(lines.size());
		System.out.println(list.size());
		Helper.pause();
		for(int i = 0; i < lines.size(); i++) {
			list.get(i).x = lines.get(i);
		}
		
		list.stream().sorted(new Comparator<Triple<String, String, Long>>() {
			public int compare(Triple<String,String,Long> t1, Triple<String,String,Long> t2) {
				return t1.y.compareTo(t2.y);
			}
		});
		
		List<String> list2 = new ArrayList<>();
		
		int i = 0;
		while (i < list.size()) {
			int end = i+1;
			String user = list.get(i).y;
			while (end < list.size() && list.get(end).y == user) {
				end++;
			}
			List<Triple<String,String,Long>> sublist = list.subList(i, end);
			List<String> sortedStrings = sublist.stream().sorted(new Comparator<Triple<String,String,Long>>() {
				public int compare(Triple<String, String, Long> t1, Triple<String, String, Long> t2) {
					return t1.z.compareTo(t2.z);
				}
			}).map(e -> e.x).collect(Collectors.toList());
			list2.addAll(sortedStrings);
			i = end;
		}
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("Users/alfordsimon/Desktop/Germain data/June28/2gether.csv"));
			for(String line: list2) {
				bw.write(line + "\n");
			}
			bw.close();
		} catch (IOException e) {
			System.out.println("File writing failed");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		makeJune28Data("simon-20170620.1.csv");
		kSubMeansStuff(75, "/Users/alfordsimon/Desktop/Germain data/June ");
		Map<Character, List<Subpoint>> map = new HashMap<>();
		for(Subpoint s: subpoints) {
			if (s.data[6].equals("ONRINGING") || s.data[6].equals("ONHANGUP") || s.data[6].equals("ONAFTERWORKDONE") || s.data[6].equals("ONANSWER")) {
				int i = findMean(s);
				Character label = labels.charAt(i);
				if (!map.containsKey(label)) {
					map.put(label, new ArrayList<>());
				}
				map.get(label).add(s);
			}
		}
		System.out.println(map.keySet().toString());
		System.out.println(map.values().stream().map(e -> Integer.toString(e.size())).collect(Collectors.joining(",")));
	}
}
