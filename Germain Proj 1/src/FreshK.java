import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;

/**
 * Class so that it works on any data thing instead of having
 * a bunch of different classes for each type.
 * @author alfordsimon
 *
 */
public class FreshK {
	public static List<Sequence> sequences;
	public static List<Point> points;
	public static int numPoints; //since points have frequencies, points.size() is just number of unique points, not overall
	        
	public static DatasetFormat format;
	public static double[] rangeNumVals;
            
	public static List<Map<String, Integer>> attrValMaps;
	public static List<List<String>> reverseAttrs;
	public static Map<Integer, Map<Tuple<Integer, Integer>, Double>> stringDistLookups;
	        
	public static List<List<Point>> finalPointClusters;
	public static Point[] finalPointMeans;
	public static final String labels = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz!@#$%^&*()-=_+{}[]|:;<,>.?/~`";

	public static void importData(String dataFilePath, DatasetFormat dsf) {
		System.out.println("Loading");
		long time = System.currentTimeMillis();
		
		FreshK.format = dsf;
		
		FreshK.rangeNumVals = new double[dsf.numAttrs];
		double[] largestNumVals = new double[dsf.numAttrs];
		double[] smallestNumVals = new double[dsf.numAttrs];
		
		for(int i = 0; i < format.numAttrs; i++) {
			largestNumVals[i] = Double.MIN_VALUE;
			smallestNumVals[i] = Double.MAX_VALUE;
		}
		
		numPoints = 0;
		sequences = new ArrayList<>();
		Map<String, List<TimedPoint>> sacs = new HashMap<>();
		points = new ArrayList<>();
		attrValMaps = new ArrayList<>(format.numAttrs);
		reverseAttrs = new ArrayList<>(format.numAttrs);
		stringDistLookups = new HashMap<>(format.numAttrs);
		
		for(int i = 0; i < format.numAttrs; i++) {
			attrValMaps.add(new HashMap<>());
			reverseAttrs.add(new ArrayList<>());
			if (format.attrTypes[i] == DatasetFormat.STR_MATCH_ATTR)  {
				stringDistLookups.put(i, new HashMap<>());
			}
		}
		
		try {
			CSVReader reader = new CSVReader(new FileReader(dataFilePath));
			for(String[] data: reader) {
				Date timestamp = null;
				String user = null;
				int[] attrs = new int[format.numAttrs]; 
				Point thisPoint = null;
				int j = 0;
				
				for(int i = 0; i < format.numData; i++) {
					if (i == format.timestampIndex) {
						timestamp = format.dp.parse(data[i]);
					} else if (i == format.userIndex) {
						user = data[format.userIndex];
					} else {
						if (!attrValMaps.get(i).containsKey(data[i])) {
							reverseAttrs.get(i).add(data[i]);
							attrValMaps.get(i).put(data[i], reverseAttrs.get(i).size() - 1); 
						}
						
						attrs[j] = attrValMaps.get(i).get(data[i]);
						j++;
					}
				}
				
				boolean foundMatch = false;
				for(int i = 0; i < points.size(); i++) {
					Point p = points.get(i);
					if (Arrays.equals(attrs, p.attrs)) {
						p.freq++;
						thisPoint = p;
						foundMatch = true;
						break;
					}
				}
				
				if (!foundMatch) {
					thisPoint = new Point(attrs);
					points.add(thisPoint);
				}
				
				if (!sacs.containsKey(user)) {
					sacs.put(user, new ArrayList<>());
				}
				
				sacs.get(user).add(new TimedPoint(timestamp, thisPoint));
				numPoints++;
			
				for(int numIndex: format.numIndeces) {
					try {
						double d = Double.valueOf(attrs[numIndex]);
						if (d > largestNumVals[numIndex]) {
							largestNumVals[numIndex] = d;
						} else if (d < smallestNumVals[numIndex]) {
							smallestNumVals[numIndex] = d;
						}
					} catch (NumberFormatException e) {
						//this is okay, it just means we got an N/A and don't count it
					} catch (ArrayIndexOutOfBoundsException e) {
						e.printStackTrace();
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for(int numIndex: format.numIndeces) {
			rangeNumVals[numIndex] = largestNumVals[numIndex] - smallestNumVals[numIndex];
		}
		
		sacs.values()
			.stream()
			.forEach(e -> sequences.add(
					new Sequence(e.stream()
								  .map(f -> f.p)
								  .collect(Collectors.toList()))));

		long time2 = System.currentTimeMillis();
		double secs = (double)(time2 - time) / 1000.0;
		System.out.println("Loading done (" + secs + " seconds)");
	}
	
	public static double stringDist(int index, int i1, int i2) {
		Tuple<Integer, Integer> t1 = new Tuple<>(i1, i2);
		if (stringDistLookups.get(index).containsKey((t1)))
			return stringDistLookups.get(index).get(t1);
		Tuple<Integer, Integer> t2 = new Tuple<>(i1, i2);

		String s1 = reverseAttrs.get(index).get(i1);
		String s2 = reverseAttrs.get(index).get(i2);
		double k = 0;
		for(int j = 0; j < Math.min(s1.length(), s2.length()); j++) {
			if (s1.charAt(j) == s2.charAt(j))
				k++;
		}
		double l = Math.max(s1.length(), s2.length());
		double dist = (l - k) / l;
		stringDistLookups.get(index).put(t1, dist);
		stringDistLookups.get(index).put(t2, dist);
		return dist;
	}
	
	public static double distance(Point p1, Point p2) {
		return distance(p1.attrs, p2.attrs);
	}
	
	public static double distance(int[] attrs1, int[] attrs2) {
		double dist = 0;
		for(int i = 0; i < format.numAttrs; i++) {
			if (format.weights[i] != 0) {
				switch (format.attrTypes[i]) {
				case DatasetFormat.CAT_ATTR:
					if (! (attrs1[i] == attrs2[i])) {
						dist += format.weights[i];
					}
					break;
					
				case DatasetFormat.NUM_ATTR:
					// Because FreshK sets rangeNumVals according to all of the points, 
					// but simpler sets rangeNumVals only according to the rep points.
					//so this could make the distances different = clusters different.
					System.err.println("Should use numeric data for distances");
					try {
						double d1 = Double.valueOf(attrs1[i]);
						double d2 = Double.valueOf(attrs2[i]);
						dist += format.weights[i]*(1.0/rangeNumVals[i])*Math.abs(d2 - d1);
					} catch (NumberFormatException e) {
						// this just means we had a "NONE" or "N/A" value, it's okay.
					}

					break;
					
				case DatasetFormat.STR_MATCH_ATTR:
					dist += format.weights[i]*stringDist(i, attrs1[i], attrs2[i]); 
					break;
				
				default:
					System.err.println("I don't think this should ever happen");
				}
			}
		}
		
		return dist;
	}
	
	public static Point[] kPointMeans(int k) {	
		Point[] means = new Point[k];
		int maxIters = 100;
		//initialize to random points
		Random rand = new Random();
		List<Point> used = new ArrayList<>();
		int n = 0;
		Point chosen = null;
		for (int i = 0; i < k; i++) {
			do {
				n = rand.nextInt(numPoints);
				int sum = 0;
				for (Point p: points) {
					sum += p.freq;
					if (sum > n) {
						chosen = p;
						break;
					}
			
				}
			} while (!used.contains(chosen));
		
			means[i] = chosen;
			used.add(chosen);
		}
		
		List<List<Point>> clusters = null;
		List<List<Point>> oldClusters;
		int iter = 1;
		
		do {
			System.out.println("iter " + iter + " / " + maxIters);
			oldClusters = clusters;
			
			clusters = new ArrayList<>(k);
			for (int i = 0; i < k; i++) {
				clusters.add(new ArrayList<>());
			}
			
			// associate each subpoint with its nearest mean
			for (Point p: points) {
				double minDistance = Integer.MAX_VALUE;
				int nearest = 0;
				for (int i = 0; i < k; i++) {
					Point mean = means[i];
					double dist = distance(p, mean);
					
					if (dist < minDistance) {
						minDistance = dist;
						nearest = i;
					}
				}
				clusters.get(nearest).add(p);
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
			Point[] newMeans = new Point[k];
			for(int i = 0; i < k; i++) {
				List<Point> cluster = clusters.get(i);
				
				int[] attrList = new int[format.numAttrs];
				//for each attribute
				for(int aIndex = 0; aIndex < format.numAttrs; aIndex++) {
					switch(format.attrTypes[aIndex]) {
					case DatasetFormat.CAT_ATTR:
						//categorical, so find the most common value and that is the value for the new mean
	
						//create frequency list for values
						Map<Integer, Integer> freqList = new HashMap<>();
						for(Point p: cluster) {
							int key = p.attrs[aIndex];
							Integer val = freqList.get(key);
							freqList.put(key, (val == null ? p.freq : val + p.freq));
						}
						
						//find most common
						int mostCommon = 0;
						int maxFreq = 0;
						for(Entry<Integer, Integer> e: freqList.entrySet()) {
							if (e.getValue() > maxFreq) {
								mostCommon = e.getKey();
								maxFreq = e.getValue();
							}
						}
						attrList[aIndex] = mostCommon;
						break;
						
					case DatasetFormat.NUM_ATTR:
						//numerical, so find the average value and use that.
						System.err.println("This is not recommended currently");
						
						double total = 0;
						double count = 0;
						double other = 0;
						for(Point p: cluster) {
							
							int val = p.attrs[aIndex]; 
							if (reverseAttrs.get(aIndex).get(val).equals("<N/A>")) {
								other += p.freq;
								continue;
							}
							count += p.freq;
							total += p.freq*Double.valueOf(reverseAttrs.get(aIndex).get(val));
						}
						
						double avg = total / count;
						
						if (other > count) {
							attrList[aIndex] = attrValMaps.get(aIndex).get("<N/A>");
						} else {
							String savg = Double.toString(avg);
							int newVal = reverseAttrs.get(aIndex).size() - 1;
							reverseAttrs.get(aIndex).add(savg);
							attrValMaps.get(aIndex).put(savg, newVal);
							attrList[aIndex] = newVal;
						}
						
						break;
					case DatasetFormat.STR_MATCH_ATTR:
						//medoids approach
						int min = -1;
						double minDist = Double.MAX_VALUE;
						for(Point p1: cluster) {
							double dist = 0;
							int i1 = p1.attrs[aIndex];
							for(Point p2: cluster) {
								int i2 = p2.attrs[aIndex];
								dist += p2.freq*stringDist(aIndex, i1, i2);
							}
							if (dist < minDist) {
								minDist = dist;
								min = i1;
							}
						}
						
						attrList[aIndex] = min;
						break;
					default: 
						System.err.println("This shouldn't happen");
					}
				}//end for each attribute

				System.out.println(cluster.size() + " " + Arrays.toString(attrList));
				newMeans[i] = new Point(attrList);
			}
			
			means = newMeans;
			iter++;
			System.out.println("Round over\n");
			System.out.println(iter <= maxIters);
		} while (iter <= maxIters && !clusters.equals(oldClusters));
			
		finalPointMeans = means;
		finalPointClusters = clusters;
		return means;
	}
	
	public static void kPointMeansAndSave(int k, String filePath) {
		kPointMeans(k);
		k = finalPointClusters.size();
		System.out.println(k);
		int[] indeces = new int[k];
		int[] sizes = new int[k];
		int[][] array = {indeces, sizes};
		int sortRow = 1;
		
		for(int i = 0; i < k; i++) {
			sizes[i] = finalPointClusters.get(i).size();
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
				for(int aIndex = 0; aIndex < format.numAttrs; aIndex++) {
					int val = finalPointMeans[i].attrs[aIndex];
					String s = reverseAttrs.get(aIndex).get(val);
					arrString += s + ",";
				}
				arrString = arrString.substring(0, arrString.length() - 1);
				dataWriter.write(arrString + "\n");
				
				Map<Point, Integer> freqMap = Helper.freqMap(finalPointClusters.get(i));
				infoWriter.write(labels.charAt(i) + "\n");
				infoWriter.write("Unique ID size = " + freqMap.size() + "\n");
				infoWriter.write(" Cluster  size = " + finalPointClusters.get(i).size()
						+ "\n");
				infoWriter.write("mean = " + finalPointMeans[i].toString() + "\n");
				infoWriter.write(getStats(finalPointClusters.get(i)) + "\n");
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
	
	public static String getStats(List<Point> cluster) {
		String stuff = "";
		
		for(int aIndex = 0; aIndex < format.numAttrs; aIndex++) {
			if (format.weights[aIndex] == 0) {
				continue;
			}
			stuff += aIndex + ": ";
			List<Integer> items = new ArrayList<>(); 
			for(Point p: cluster) {
				items.add(p.attrs[aIndex]);
			}
			
			Map<Integer, Integer> freqMap = Helper.sortByValue(Helper.freqMap(items));
			Integer[] keyArr = freqMap.keySet().toArray(new Integer[freqMap.size()]);
			for(int j = 0; j < Math.min(10, freqMap.size()); j++) {
				int val = keyArr[freqMap.size() - 1 - j];
				String str = reverseAttrs.get(aIndex).get(val);
				DecimalFormat df = new DecimalFormat("#.##");
				stuff += str + " " + df.format((double)100*freqMap.get(val) / (double)cluster.size()) + ", ";
			}
			stuff += "\n";
		}
		
		return stuff.substring(0, stuff.length() - 2);
	}
	
	public static int findMean(Point p) {
		int i = -1;
		double minDist = Double.MAX_VALUE;
		for(int j = 0; j < finalPointMeans.length; j++) {
			Point mean = finalPointMeans[j];
			double dist = distance(p, mean);
			if (dist < minDist) {
				minDist = dist;
				i = j;
			}
		}
		return i;
	}
	
	public static void main(String[] args) {
		importData("/Users/alfordsimon/Desktop/Germain data/June/simon-20170620.1.csv", DatasetFormat.JUNE28);
		kPointMeansAndSave(75, "/Users/alfordsimon/Desktop/Germain data/June ");
		Map<Character, List<Point>> map = new HashMap<>();
		for(Point p: points) {
			String str = reverseAttrs.get(6).get(p.attrs[6]);
			if (str.equals("ONRINGING") || str.equals("ONHANGUP") || str.equals("ONAFTERWORKDONE") || str.equals("ONANSWER")) {
				int i = findMean(p);
				Character label = labels.charAt(i);
				if (!map.containsKey(label)) {
					map.put(label, new ArrayList<>());
				}
				map.get(label).add(p);
			}
		}
		System.out.println(map.keySet().toString());
		System.out.println(map.values().stream().map(e -> Integer.toString(e.size())).collect(Collectors.joining(",")));
	}
}
