import java.util.Date;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Scanner;


public class BKMeans {
	public static String[] attribute_labels;
	public static ArrayList<BSequence> data;
	public static ArrayList<String> lines;
	public static double largestDuration;
	
// public static void saveData(ArrayList<BSequence> data, int maxSize, String filename) {
//		String s = "\"UXT_START_TIME\",\"IPV4_ADDR\",\"USERNAME\",\"URL_PATH\",\"CATEGORY\",\"NAME\",\"NAME2\",\"UXT_METHOD\",\"UXT_STATUS\",\"UXT_DURATION\",\"'<N/A>'\n";
//	    s += maxSize + "\n";
//		int d = 0;
//		System.out.println(data.size());
//		for(BSequence bs: data) {
//			d++;
//			System.out.println(d);
//			for(BSubpoint bsp: bs.data) {
//				s += lines.get(bsp.id) + "\n";
//			}
//			s += "new sequence \n";
//		}
//		
//		FileWriter fw;
//		try {
//			fw = new FileWriter(filename);
//			fw.write(s);
//			fw.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//	
//	public static ArrayList<BSequence> loadData(String filePath) {
//		ArrayList<String> list = TxtParser.parseFile(filePath);
//		attribute_labels = list.remove(0).split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
//		int numAttrs = attribute_labels.length;
//		int maxSize = Integer.valueOf(list.remove(1));
//		ArrayList<BSequence> data = new ArrayList<BSequence>();
//		ArrayList<BSubpoint> seq = new ArrayList<BSubpoint>();
//		
//		int l = 0;
//		int snum = 0;
//		do {
//			l++;
//			String line = list.get(l);
//			if (line.equals("new sequence")) {
//				snum++;
//				for(int j = data.get(snum).data.size(); j < maxSize; j++) {
//					data.get(snum).add(new BSubpoint(numAttrs));
//				}
//				data.add(new BSequence());
//				continue;
//			} else {
//				data.get(snum).add(new BSubpoint(line));
//			}
//			
//		} while (l < list.size());
//		
//		return data;
//	}
	
	public static ArrayList<BSequence> makeData(String filePath) {
		System.out.println("loading data");
		lines = TxtParser.parseFile(filePath);
		attribute_labels = lines.remove(0).split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

		ArrayList<BSubpoint> list2 = new ArrayList<BSubpoint>(lines.size());
		for (int i = 0; i < lines.size(); i++) {
			String s = lines.get(i);
			BSubpoint sp = new BSubpoint(s, i);
			list2.add(sp);
		}
		
		System.out.println("loading 2");
		
		largestDuration = 0;
		for(BSubpoint sp: list2) {
			try {
				if (Double.valueOf(sp.data[9]) > largestDuration) {
					largestDuration = Double.valueOf(sp.data[9]);
				}
			} catch (Exception e) {
				e.printStackTrace(System.out);
				System.exit(0);
			}
		}
		
		System.out.println("loading 3");
		
		//now cluster by source address = s.data[1]
		LinkedHashMap<String, ArrayList<BSubpoint>> sacs = new LinkedHashMap<String, ArrayList<BSubpoint>>();
		for (BSubpoint s: list2) {
			//System.out.println(s.data[0]);
			if (sacs.containsKey(s.data[1])) {
				sacs.get(s.data[1]).add(s);
			} else {
				ArrayList<BSubpoint> a = new ArrayList<BSubpoint>();
				a.add(s);
				sacs.put(s.data[1], a);
			}
		}
		
		System.out.println("loading 4");
		
		//81 different clusters = BSequences. half of these have length 1. One has length 234.
		
		//now we have to order each cluster's data into a BSequence. 
		ArrayList<BSequence> data = new ArrayList<BSequence>(sacs.size());
		int max = 0;
		
		for(String key: sacs.keySet()) {
			ArrayList<BSubpoint> cluster = sacs.get(key);
			Collections.sort(cluster);
			data.add(new BSequence(cluster));
			if (cluster.size() > max)
				max = cluster.size();
			
		}
		//now the clusters are /BSequences/ !
		
//		//now add on empty BSubpoints so that all of the BSequences have the same length
//		for(BSequence s: data) {
//			while(s.data.size() < max) {
//				int numAttrs = attribute_labels.length;
//				s.data.add(new BSubpoint(numAttrs));
//			}
//		}
		
		System.out.println(max);
	
		
		BKMeans.data = data;
		System.out.println("data loaded");
		return data;
	}
	
	public static Date makeDate(String s) {
		System.out.println(s);
		String apm = s.substring(s.length() - 3);
		s = s.substring(0, s.length() - 9);
		s = s + apm;
		System.out.println(s);
		SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy hh.mm.ss.SSS a");
		Date d = null;
		try {
			d = format.parse(s);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(d.toString());
		pause();
		return d;
	}
	
	/**
	 * Returns the cluster means from the k means algorithm.
	 * @param data
	 * @param k
	 * @return
	 */
	public static BSequence[] kMeans(ArrayList<BSequence> data, int k) {
		System.out.println("k-means");
		int numIters = -1;
		
		int numAttrs = data.get(0).data.get(0).data.length;
		
		//initialize the k means to random BSequences
		BSequence[] means = new BSequence[k];
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
		
		
		BSequence[] oldMeans = new BSequence[k];
		ArrayList<ArrayList<BSequence>> clusters = null;
		ArrayList<ArrayList<BSequence>> oldClusters;
		
		do { //repeat until convergence
			numIters++;
			System.out.println("new round");
			
			oldClusters = clusters;
			clusters = new ArrayList<ArrayList<BSequence>>(k);
			for (int i = 0; i < k; i++) {
				clusters.add(new ArrayList<BSequence>());
			}
			
			// associate each BSequence with its nearest mean
			for (BSequence s: data) {
				double minDistance = Integer.MAX_VALUE;
				int nearest = 0;
				for (int i = 0; i < k; i++) {
					BSequence mean = means[i];
					double dist = s.distance(mean);
					if (dist < minDistance) {
						minDistance = dist;
						nearest = i;
					}
				}
				clusters.get(nearest).add(s);
			}
			
			// update means by averaging its associates
			
			// currently doing this by using most common categorical value for each / average numerical value
			// alternative is to use BSequence in data that minimizes total distance (k-medoids)
			// think about: average of oppositely facing zigzags is a straight line.
			BSequence[] newMeans = new BSequence[k];	
			
			//for each new mean
			for(int i = 0; i < k; i++) {
				System.out.println("k" + (i+1));
				ArrayList<BSequence> cluster = clusters.get(i);
				System.out.println("cluster = " + cluster.size());
				
				//how long to make new BSequence? Average length, for now.
				int l = 0;
				for(BSequence s: cluster) {
					l += s.data.size();
				}
				l /= cluster.size(); //average length
				
				ArrayList<BSubpoint> newSeqPoints = new ArrayList<BSubpoint>(l);
				
				//for each BSubpoint
				//System.out.println("l = " + l);
				for(int j = 0; j < l; j++) {
					//System.out.println("j=" + j);
					String[] attrList = new String[numAttrs];
					//for each attribute
					for(int attr = 0; attr < numAttrs; attr++) {
						switch(BSubpoint.compareTypes[attr]) {
						case 0:
							//categorical, so find the most common value and that is the value for the new mean
							
							//create frequency list for values
							HashMap<String, Integer> freqList = new HashMap<String, Integer>();
							for(BSequence s: cluster) {
								if (s.data.size() <= j)
									continue;
								
								String key = s.data.get(j).data[attr];
								if (freqList.containsKey(key)) {
									freqList.put(key, freqList.get(key) + 1);
								} else {
									freqList.put(key, 1);
								}
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
							for(BSequence s: cluster) {
								if (s.data.size() <= j)
									continue;
								
								String val = s.data.get(j).data[attr];
								if (val.equals("<N/A>")) {
									other++;
									continue;
								}
								count++;
								try {
									total += Double.valueOf(val);
								} catch (Exception e) {
									System.out.println(attr);
									System.out.println(s.data.get(j).toString());
									e.printStackTrace(System.out);
									System.exit(0);
								}
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
					//System.out.println(Arrays.toString(attribute_labels));
					//System.out.println(Arrays.toString(attrList));
					BSubpoint sub = new BSubpoint(attrList);
					newSeqPoints.add(sub);
				}//end for each BSubpoint
				
				BSequence newMean = new BSequence(newSeqPoints);
				newMeans[i] = newMean;
				//print(newMean);
			}//end for each mean
			
			//new Scanner(System.in).nextLine();
		} while (!clusters.equals(oldClusters));
		
		if(numIters > 1) {
			new Scanner(System.in).nextLine();
		}
		
		return means;
	}
	
	public static void print(BSequence seq) {
		for(BSubpoint sub: seq.data) {
			System.out.println(sub.toString());
		}
	}
	
	public static void testReflexivity(ArrayList<BSequence> data) {
		for(BSequence s: data) {
			if (s.distance(s) != 0) {
				System.out.println(s.toString());
				new Scanner(System.in).nextLine();
			}
		}
	}
	
	public static void testSymmetricity(ArrayList<BSequence> data) {
		System.out.println(BSubpoint.maxDist);
		for(BSequence s1: data) {
			for(BSequence s2: data) {
				if (s1.distance(s2) != s2.distance(s1)) {
					System.out.println(s1.distance(s2));
					System.out.println(s2.distance(s1));
					System.out.println(s1.toString());
					System.out.println(s2.toString());
					new Scanner(System.in).nextLine();
				}
			}
		}
	}
	
	public static void testTriangleEquality(ArrayList<BSequence> data) {
		//triangle inequality
		for (BSequence s1: data) {
			for (BSequence s2: data) {
				for (BSequence s3: data) {
					//System.out.println("new");
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
						System.out.println(s1.data.size());
						System.out.println(s2.data.size());
						System.out.println(s3.data.size());
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
	
	public static void testDist(ArrayList<BSequence> data) {
		//testReflexivity(data);
		System.out.println("reflex good");
		//testSymmetricity(data);
		System.out.println("symmet good");
		testTriangleEquality(data);
		System.out.println("tri good");

	}
	
	public static void pause() {
		new Scanner(System.in).nextLine();
	}
	
	public static void print(String item) {
		System.out.println(item);
	}
	
	public static void main(String[] args) {
		ArrayList<BSequence> data = BKMeans.makeData("First_Big.csv");
		
		//testDist(data);
		
		while(true) {
			BSequence[] means = BKMeans.kMeans(data, 3);
		}
	}
}
