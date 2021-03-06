import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
	
	public static DatasetFormat format;
	public static double[] rangeNumVals;
	public static int numPoints;
	
	public static List<List<Seq>> finalClusters;
	public static Seq[] finalMeans;
	
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
			
			double sizePenalty = (maxSize - minSize)*Simpler.format.maxDist;
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
			return Levenshtein.softLevi(s1, s2, Simpler.format.maxDist);
		}
	};
	
	
	/**
	 * @param dataFilePath -string to data file
	 * @param keyFilePath - string to file turning points into pointtypes
	 * @param weights - only for the equals data section
	 * @param attrTypes - 3 = timestamp, 4 = user, 0 = categorical, 1 = numerical, 2 = string matching  
	 * @param dp - converts timestamp string into date
	 */
	public static void importReps(String dataFilePath, String infoFilePath, DatasetFormat format) {
		System.out.println("Loading reps");
		long time = System.currentTimeMillis();
		
		Simpler.format = format;
		rangeNumVals = new double[format.numAttrs];
		double[] largestNumVals = new double[format.numAttrs];
		double[] smallestNumVals = new double[format.numAttrs];
		
		
		for(int i = 0; i < format.numAttrs; i++) {
			largestNumVals[i] = Double.MIN_VALUE;
			smallestNumVals[i] = Double.MAX_VALUE;
		}
		
		ArrayList<Integer> numIndeces = new ArrayList<>();
		for(int i = 0; i < format.numAttrs; i++) {
			int type = format.attrTypes[i];
			if (type == DatasetFormat.NUM_ATTR) {
				numIndeces.add(i);
			}
		}
		
		try {
			List<String> infos = new ArrayList<>();
			BufferedReader br = new BufferedReader(new FileReader(infoFilePath));
			String line = "";
			while(line != null) {
				String info = "";
				while((line = br.readLine()) != null && !line.equals("end info")) {
					info += line + '\n';
				}
				infos.add(info);
			}
			br.close();
			
			pointTypes = new PointType[infos.size()];
			CSVReader reader = new CSVReader(new FileReader(dataFilePath));
			boolean first = true;
			int pointIndex = 0;
			for(String[] data: reader) {
				// skip the first item
				if (first) {
					first = false;
					continue;
				}
				
				String[] attrs = new String[data.length -2];
				int j = 0;
				for (int i = 0; i < data.length; i++) {
					if (i != format.timestampIndex && i != format.userIndex) {
						attrs[j] = data[i];
						j++;
					}
				}
				
				PointType pt = new PointType(attrs, infos.get(pointIndex));
				pointTypes[pointIndex] = pt;
				pointIndex++;
				
				for(int numIndex: numIndeces) {
					try {
						double d = Double.valueOf(pt.attrs[numIndex]);
						if (d > largestNumVals[numIndex]) {
							largestNumVals[numIndex] = d;
						} else if (d < smallestNumVals[numIndex]) {
							smallestNumVals[numIndex] = d;
						}
					} catch (NumberFormatException e) {
						//this is okay, it just means we got an N/A and don't count it
					} catch (ArrayIndexOutOfBoundsException e) {
						System.out.println(Arrays.toString(pt.attrs));
						e.printStackTrace();
					}
				}
			}
			
			numPoints = pointTypes.length;
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for(int numIndex: numIndeces) {
			rangeNumVals[numIndex] = largestNumVals[numIndex] - smallestNumVals[numIndex];
		}
		
		distanceLookup = new double[numPoints][numPoints];
		for(int i = 0; i < pointTypes.length; i++) {
			for(int j = 0; j < pointTypes.length; j++) {
				double dist = distance(pointTypes[i].attrs, pointTypes[j].attrs);
				distanceLookup[i][j] = dist;
				distanceLookup[j][i] = dist;
			}
		}
		
		long time2 = System.currentTimeMillis();
		double secs = (double)(time2 - time) / 1000.0;
		System.out.println("Loading done (" + secs + " seconds)");
	}
	
	public static void importAll(String filePath) {
		System.out.println("Loading all");
		long time = System.currentTimeMillis();
		
		Map<String, List<PointType>> sacs = new HashMap<>();
		
		try {
			CSVReader reader = new CSVReader(new FileReader(filePath));
			boolean first = true;
			for(String[] data: reader) {
				// skip the first item
				if (first) {
					first = false;
					continue;
				}
				
				String[] attrs = new String[data.length -2];
				String user = "";
				int j = 0;
				for (int i = 0; i < data.length; i++) {
					if (format.attrTypes[i] == format.userIndex) {
						user = data[i];
					} else if (format.attrTypes[i] == format.timestampIndex) {
						
					} else {
						attrs[j] = data[i];
						j++;
					}
				}
				
				PointType nearest = nearestRep(attrs);
				
				if (!sacs.containsKey(user)) {
					sacs.put(user, new ArrayList<>());
				}
				
				sacs.get(user).add(nearest);
			}
			
			sequences = sacs.entrySet().stream().map(e -> new Seq(e.getValue(), e.getKey())).collect(Collectors.toList());
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long time2 = System.currentTimeMillis();
		double secs = (double)(time2 - time) / 1000.0;
		System.out.println("Loading done (" + secs + " seconds)");

	}
	
	public static PointType nearestRep(String[] attrList) {
		PointType nearest = null;
		double minDist = Double.MAX_VALUE;
		
		for(PointType pt: pointTypes) {
			double dist = distance(attrList, pt.attrs);
			if (dist < minDist) {
				minDist = dist;
				nearest = pt;
			}
		}
		
		return nearest;
	}
	
	public static double distance(String[] l1, String[] l2) {
		double dist = 0;
		for(int i = 0; i < format.numAttrs; i++) {
			if (format.weights[i] != 0) {
				//attrTypes - 3 = timestamp, 4 = user, 0 = categorical, 1 = numerical, 2 = string matching
				switch (format.attrTypes[i]) {
				case 3:
					System.err.println("This weight should be zero " + i);
					
				case 4:
					System.err.println("This weight should be zero " + i);
					
				case 0:
					if (!l1[i].equals(l2[i])) {
						dist += format.weights[i];
					}
					break;
					
				case 1:
					// Because FreshK sets rangeNumVals according to all of the points, 
					// but simpler sets rangeNumVals only according to the rep points.
					//so this could make the distances different = clusters different.
					System.err.println("Should use numeric data for distances");
					boolean b1 = l1[i].equals("<none>") || l1[i].equals("<N/A>");
					boolean b2 = l2[i].equals("<none>") || l2[i].equals("<N/A>");

					if (!b1 && !b2) {
						dist += format.weights[i]*(1.0/rangeNumVals[i])*Math.abs(Double.valueOf(l1[i]) - Double.valueOf(l2[i]));
					}
					
					break;
					
				case 2:
					dist += format.weights[i]*stringDist(l1[i], l2[i]); 
					break;
					
				default:
					System.err.println("Unknown attr type: " + format.attrTypes[i]);
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
		List<List<Seq>> clusters = null;
		List<List<Seq>> oldClusters;
		
		do { //repeat until convergence
			//System.out.println("New round");
			
			oldClusters = clusters;
			clusters = cluster(means, dm);
			
			//update means by choosing member that minimizes distance
			Seq[] newMeans = new Seq[k];
			for(int i = 0; i < k; i++) {
			//	System.out.println("k" + (i+1));
				List<Seq> cluster = clusters.get(i);
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
	
	private static List<List<Seq>> cluster(Seq[] means, DistanceMetric<Seq> dm) {
		int k = means.length;
		List<List<Seq>> clusters = new ArrayList<>(k);
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
	
	private static Seq medoid(List<Seq> cluster, DistanceMetric<Seq> dm) {
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
	
	public static void makeJune28Data(int k, String fileName) {
		String filePath = "/Users/alfordsimon/Desktop/Germain data/June 28/" + fileName;
		String repFilePath = "/Users/alfordsimon/Desktop/Germain data/June K-means_" + k + "points_data.txt";
		String infoFilePath = "/Users/alfordsimon/Desktop/Germain data/June K-means_" + k + "points_info.txt";
		
		importReps(repFilePath, infoFilePath, DatasetFormat.JUNE28);
		importAll(filePath);
	}
	
	public static void exportSeqs(String fileName) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
			for(Seq s: sequences) {
				bw.write(s.toString() + "\n");
			}
			bw.close();
			System.out.println("Exported " + fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void makeFirstData(int k, String fileName) {
		String filePath = "/Users/alfordsimon/Desktop/Germain Data/First/" + fileName;
		String repFilePath = "K-means_" + k + "points_data.txt";
		String infoFilePath = "K-means_" + k + "points_info.txt";
		
		importReps(repFilePath, infoFilePath, DatasetFormat.FIRST);
		importAll(filePath);
	}
	
	public static void main(String[] args) {
		int k = 58;
		makeJune28Data(k,"simon-20170620.1.csv");
		exportSeqs("/Users/alfordsimon/Desktop/Germain Data/seqed" + k + "-June20.1.txt");
	}
}