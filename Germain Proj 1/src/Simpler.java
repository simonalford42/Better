import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;

public class Simpler {
	public static List<Seq> sequences;
	public static double[][] distanceLookup;
	public static PointType[] pointTypes;
	
	public static int[] attrTypes;
	public static double[] weights;
	public static double[] rangeNumVals;
	public static int numAttrs;
	public static double maxDist;
	public static DateParser dateParser;
	public static int timeStampIndex;
	public static int userIndex;
	
	public static ArrayList<ArrayList<Seq>> finalClusters;
	public static Seq[] finalMeans;
	public static ArrayList<ArrayList<PointType>> finalSubClusters;
	public static Integer[] finalSubMeans;
	
	public static DistanceMetric<Seq> defaultDist = new DistanceMetric<Seq>() {
		public double distance(Seq s1, Seq s2) {
			if(s1 == s2)
				return 0;
			
			//this way nothing is 0 from anything but itself. Changing this val shouldn't affect a ton.
			double distance = 1;
			
			int minSize = Math.min(s1.length, s2.length);
			int maxSize = Math.max(s1.length, s2.length);
			
			for(int i = 0; i < minSize; i++) {
				PointType th = s1.data.get(i);
				PointType ot = s2.data.get(i);
				double subdist = Simpler.distance(th, ot);
				distance += subdist;
			}
			
			double sizePenalty = (maxSize - minSize)*Simpler.maxDist;
			distance += sizePenalty;
			distance /= (double)maxSize;
			
			return distance;
		}
	};
	
	public static DistanceMetric<Seq> levDist = new DistanceMetric<Seq>() {
		public double distance(Seq s1, Seq s2) {
			return Levenshtein.hardLevinshtein(s1, s2, 1);
		}
	};
	
	public static DistanceMetric<Seq> softLevDist = new DistanceMetric<Seq>() {
		public double distance(Seq s1, Seq s2) {
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
	public static void importReps(String filePath, double[] weights, int[] attrTypes, DateParser dp) {
		System.out.println("Loading");
		long time = System.currentTimeMillis();
		
		numAttrs = attrTypes.length;
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
		
		userIndex = -1;
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
		
		try {
			CSVReader reader = new CSVReader(new FileReader(filePath));
			pointTypes = reader.readAll().stream().skip(1).map(e -> new PointType(e)).toArray(size -> new PointType[size]);
			System.out.println("halfway?");
			
			for(PointType pt: pointTypes) {
				for(int numIndex: numIndeces) {
					try {
						double d = Double.valueOf(pt.data[numIndex]);
						if (d > largestNumVals[numIndex]) {
							largestNumVals[numIndex] = d;
						} else if (d < smallestNumVals[numIndex]) {
							smallestNumVals[numIndex] = d;
						}
					} catch (NumberFormatException e) {
						//this is okay, it just means we got an N/A and don't count it
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println(Arrays.toString(pt.data));
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
		
		long time2 = System.currentTimeMillis();
		double secs = (double)(time2 - time) / 1000.0;
		
		for(int i = 0; i < pointTypes.length; i++) {
			for(int j = 0; j < pointTypes.length; j++) {
				double dist = distance(pointTypes[i].data, pointTypes[j].data);
				distanceLookup[i][j] = dist;
				distanceLookup[j][i] = dist;
			}
		}
		
		System.out.println("Loading done (" + secs + " seconds)");
	}
	
	public static void importAll(String filePath) {
		Map<String, ArrayList<PointType>> sacs = new HashMap<>();
		
		try {
			CSVReader reader = new CSVReader(new FileReader(filePath));
			
			for(String[] attrList: reader) {
				PointType nearest = nearestRep(attrList);
				String user = attrList[userIndex];
				
				if (!sacs.containsKey(user)) {
					sacs.put(user, new ArrayList<>());
				}
				
				sacs.get(user).add(nearest);
			}
			
			sequences = sacs.values().stream().map(e -> new Seq(e)).collect(Collectors.toList());
			reader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static PointType nearestRep(String[] attrList) {
		PointType nearest = null;
		double minDist = Double.MAX_VALUE;
		
		for(PointType pt: pointTypes) {
			double dist = distance(attrList, pt.data);
			if (dist < minDist) {
				minDist = dist;
				nearest = pt;
			}
		}
		
		return nearest;
	}

	public static double distance(String[] l1, String[] l2) {
		double dist = 0;
		for(int i = 0; i < numAttrs; i++) {
			if (weights[i] != 0) {
				//attrTypes - 3 = timestamp, 4 = user, 0 = categorical, 1 = numerical, 2 = string matching
				switch (attrTypes[i]) {
				case 3:
					break;
					
				case 4:
					break;
					
				case 0:
					if (l1[i].equals(l2[i])) {
						dist += weights[i];
					}
					break;
					
				case 1:
					boolean b1 = l1[i].equals("<none>") || l1[i].equals("<N/A>");
					boolean b2 = l2[i].equals("<none>") || l2[i].equals("<N/A>");

					if (!b1 && !b2) {
						dist += weights[i]*(1.0/FreshK.rangeNumVals[i])*Math.abs(Double.valueOf(l1[i]) - Double.valueOf(l2[i]));
					}
					
					break;
					
				case 2:
					dist += weights[i]*stringDist(l1[i], l2[i]); 
					break;
					
				default:
					throw new java.lang.RuntimeException("this not right");
				}
			}
		}
		
		return dist;
	}
	
	public static double stringDist(String s1, String s2) {
		double hits = 0;
		
		for(int j = 0; j < Math.min(s1.length(), s2.length()); j++) {
			if (s1.charAt(j) == s2.charAt(j)) {
				hits++;
			}
		}
		
		double maxL = Math.max(s1.length(), s2.length());
		return (maxL - hits) / maxL;
	}

	public static double distance(PointType p1, PointType p2) {
		return distanceLookup[p1.id][p2.id];
	}
	
	private static Seq[] initMeans(int k) {
		Seq[] means = new Seq[k];
		Random rand = new Random();
		Set<Integer> used = new HashSet<>();
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
	
	public static Seq[] kMedoids(int k, DistanceMetric<Seq> dm) {
		System.out.println("K-medoids");
		//initialize the k means to random FreshSeqs
		Seq[] means = initMeans(k);
		ArrayList<ArrayList<Seq>> clusters = null;
		ArrayList<ArrayList<Seq>> oldClusters;
		
		do { //repeat until convergence
			//System.out.println("New round");
			
			oldClusters = clusters;
			clusters = cluster(means, dm);
			
			//update means by choosing member that minimizes distance
			Seq[] newMeans = new Seq[k];
			for(int i = 0; i < k; i++) {
			//	System.out.println("k" + (i+1));
				ArrayList<Seq> cluster = clusters.get(i);
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
	
	private static ArrayList<ArrayList<Seq>> cluster(Seq[] means, DistanceMetric<Seq> dm) {
		int k = means.length;
		ArrayList<ArrayList<Seq>> clusters = new ArrayList<ArrayList<Seq>>(k);
		for (int i = 0; i < k; i++) {
			clusters.add(new ArrayList<Seq>());
		}
		
		// associate each Seq with its nearest mean
		for (Seq s: sequences) {
			double minDistance = Integer.MAX_VALUE;
			int nearest = 0;
			for (int i = 0; i < k; i++) {
				Seq mean = means[i];
				double dist = dm.distance(s, mean);
				
				if (dist < minDistance) {
					minDistance = dist;
					nearest = i;
				}
			}
			clusters.get(nearest).add(s);
		}
		
		return clusters;
	}
	
	private static Seq medoid(ArrayList<Seq> cluster, DistanceMetric<Seq> dm) {
		double[] totalDistances = new double[cluster.size()];
		double minDist = Integer.MAX_VALUE;
		Seq min = cluster.get(0);
		
		for(int j = 0; j < cluster.size(); j++) {
			double dist = 0;
			Seq fs = cluster.get(j);
			
			for(Seq fs2: cluster) {
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
		Scanner s = new Scanner(System.in);
		s.nextLine();
		s.close();
	}
	
	public static void makeNewData(int k) {
		double[] weights = {0, 		0, 		  0,  			1,  		1,	 	1, 		1, 		 1, 		1, 			1, 				0};
		//		UXT_START_TIME	IPV4_ADDR	USERNAME	URL_PATH	CATEGORY	NAME	NAME2	UXT_METHOD	UXT_STATUS	UXT_DURATION	<N/A>'
		// 0 is for categorical data, 1 is for numerical,
		int[] dataTypes = {3,0,4, 0,0,0, 0,0,0, 1, 0};	
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
		String repFilePath = "K-means_" + k + "points.txt";
		importReps(repFilePath, weights, dataTypes, dp);
		importAll(filePath);
	}
	
	public static void main(String[] args) {
		makeNewData(10);
		List<String> data = sequences.stream().map(e -> e.data.stream().map(f -> f.label).collect(Collectors.joining(""))).collect(Collectors.toList());
		String[] common = PatternFinder.findMostCommon(data, 5, 15);
		Arrays.stream(common).forEach(System.out::println);
	}
}