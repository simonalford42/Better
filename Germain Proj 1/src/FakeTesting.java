import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Scanner;


public class FakeTesting {
	public static String[] attribute_labels = {"uid","time","a"};
	public static ArrayList<FakeSequence> data;
	
	/**
	 * For when the fake data is in the format AAAB\nBBBA etc.
	 */
	public static ArrayList<FakeSequence> makeData2(String filePath) {
		ArrayList<String> list = TxtParser.parseFile(filePath);
		ArrayList<FakeSequence> list2 = new ArrayList<FakeSequence>(list.size());
		for(String s: list) {
			ArrayList<FakeSubpoint> d = new ArrayList<FakeSubpoint>();
			for(int i = 0; i < s.length(); i++) {
				d.add(new FakeSubpoint("fun", i, String.valueOf(s.charAt(i))));
			}
			list2.add(new FakeSequence(d));
		}
		
		return list2;
		
	}
	
	public static ArrayList<FakeSequence> makeData(String filePath) {
		ArrayList<String> list = TxtParser.parseFile(filePath);
		
		ArrayList<FakeSubpoint> list2 = new ArrayList<FakeSubpoint>(list.size());
		for (String s: list) {
			FakeSubpoint sp = new FakeSubpoint(s);
			list2.add(sp);
		}
		
		//now cluster by source address = s.data[1]
		LinkedHashMap<String, ArrayList<FakeSubpoint>> sacs = new LinkedHashMap<String, ArrayList<FakeSubpoint>>();
		for (FakeSubpoint s: list2) {
			//System.out.println(s.data[0]);
			if (sacs.containsKey(s.data[0])) {
				sacs.get(s.data[0]).add(s);
			} else {
				ArrayList<FakeSubpoint> a = new ArrayList<FakeSubpoint>();
				a.add(s);
				sacs.put(s.data[0], a);
			}
		}
		
		int[] sizes = new int[sacs.size()];
		int i = 0;
		
		for(String key: sacs.keySet()) {
			sizes[i] = sacs.get(key).size();
			//System.out.println(key + " " + sizes[i]);
			//System.out.println(sacs.get(key).toString());
			i++;
		}
		
		i = 0;
		for(String key: sacs.keySet()) {
			//System.out.println(key + ": " + sizes[i]);
		}
		
		//System.out.println(sacs.size());
		//System.out.println(Arrays.toString(sizes));
		/*{1: 39,
		 * 2: 5,
		 * 3: 13,
		 * 4: 9, 
		 * 5: 3, 
		 * 6: 4, 
		 * 7, 8, 9, 14, 16, 19, 20, 234: 1
		 */
		
		//81 different clusters = FakeSequences. half of these have length 1. One has length 234.
		
		//now we have to order each cluster's data into a FakeSequence. 
		ArrayList<FakeSequence> data = new ArrayList<FakeSequence>(sacs.size());
		int max = 0;
		
		for(String key: sacs.keySet()) {
			ArrayList<FakeSubpoint> cluster = sacs.get(key);
			Collections.sort(cluster);
			data.add(new FakeSequence(cluster));
			if (cluster.size() > max)
				max = cluster.size();
			
		}
		//now the clusters are /FakeSequences/ !
		
		
		FakeTesting.data = data;
		return data;
	}
	
	public static Date makeDate(String s) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date d = null;
		try {
			d = format.parse(s);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return d;
	}
	
	public static FakeSequence[] kMedoids(ArrayList<FakeSequence> data, int k) {
		System.out.println("k-medoids");
		int numIters = -1;
		
		int numAttrs = data.get(0).data.get(0).data.length;
		
		//initialize the k means to random FakeSequences
		FakeSequence[] means = new FakeSequence[k];
		means[0] = data.get(0);
		means[1] = data.get(4);
				
//		Random rand = new Random();
//		ArrayList<Integer> used = new ArrayList<Integer>();
//		int n = 0;
//		for (int i = 0; i < k; i++) {
//			do {
//				n = rand.nextInt(data.size());
//			} while (used.contains(n));
//			
//			means[i] = data.get(n);
//			used.add(n);
//		}
		
		System.out.println("Initial means " + Arrays.toString(means));
		
		
		FakeSequence[] oldMeans = new FakeSequence[k];
		ArrayList<ArrayList<FakeSequence>> clusters = null;
		ArrayList<ArrayList<FakeSequence>> oldClusters;
		
		do { //repeat until convergence
			numIters++;
			System.out.println("new round");
			
			oldClusters = clusters;
			clusters = new ArrayList<ArrayList<FakeSequence>>(k);
			for (int i = 0; i < k; i++) {
				clusters.add(new ArrayList<FakeSequence>());
			}
			
			// associate each FakeSequence with its nearest mean
			for (FakeSequence s: data) {
				//System.out.println(s.toString());
				double minDistance = Integer.MAX_VALUE;
				int nearest = 0;
				for (int i = 0; i < k; i++) {
					FakeSequence mean = means[i];
					double dist = s.distance(mean);
					//System.out.println(" " + mean.toString() + " " + dist);
					if (dist < minDistance) {
						minDistance = dist;
						nearest = i;
					}
				}
				clusters.get(nearest).add(s);
			}
			
			//System.out.println(clusters.get(0).toString());
			//System.out.println(clusters.get(1).toString());
			
			// update means by choosing the member of cluster that minimizes total distance to others
			
			FakeSequence[] newMeans = new FakeSequence[k];
	
			//for each cluster
			for(int i = 0; i < k; i++) {
				System.out.println("k" + (i+1));
				ArrayList<FakeSequence> cluster = clusters.get(i);
				System.out.println("cluster = " + cluster.size());
				
				double[] totalDistances = new double[cluster.size()];
				double minDist = Integer.MAX_VALUE;
				FakeSequence min = cluster.get(0);
				for(int j = 0; j < cluster.size(); j++) {
					double dist = 0;
					FakeSequence fs = cluster.get(j);
					//System.out.println(fs.toString());
					for(FakeSequence fs2: cluster) {
						double d = fs.distance(fs2);
						//System.out.println(" " + fs2.toString() + " " + d);
						dist += d;
					}
					//System.out.println(" " + dist);

					totalDistances[j] = dist;
					if (dist < minDist) {
						minDist = dist;
						min = fs;
					}
				}
				
				newMeans[i] = min;
			}//end for each mean
			
			System.out.println(Arrays.toString(newMeans));
			means = newMeans;
			//new Scanner(System.in).nextLine();
		} while (!clusters.equals(oldClusters));
		
		if(numIters > 1) {
			new Scanner(System.in).nextLine();
		}
		System.out.println("Final^^");
		new Scanner(System.in).nextLine();
		return means;
	}
	
	/**
	 * Returns the cluster means from the k means algorithm.
	 * @param data
	 * @param k
	 * @return
	 */
	public static FakeSequence[] kMeans(ArrayList<FakeSequence> data, int k) {
		System.out.println("k-means");
		int numIters = -1;
		
		int numAttrs = data.get(0).data.get(0).data.length;
		
		//initialize the k means to random FakeSequences
		FakeSequence[] means = new FakeSequence[k];
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
		
		System.out.println("Initial means " + Arrays.toString(means));
		
		
		FakeSequence[] oldMeans = new FakeSequence[k];
		ArrayList<ArrayList<FakeSequence>> clusters = null;
		ArrayList<ArrayList<FakeSequence>> oldClusters;
		
		do { //repeat until convergence
			numIters++;
			System.out.println("new round");
			
			oldClusters = clusters;
			clusters = new ArrayList<ArrayList<FakeSequence>>(k);
			for (int i = 0; i < k; i++) {
				clusters.add(new ArrayList<FakeSequence>());
			}
			
			// associate each FakeSequence with its nearest mean
			for (FakeSequence s: data) {
				double minDistance = Integer.MAX_VALUE;
				int nearest = 0;
				for (int i = 0; i < k; i++) {
					FakeSequence mean = means[i];
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
			// alternative is to use FakeSequence in data that minimizes total distance (k-medoids)
			// think about: average of oppositely facing zigzags is a straight line.
			FakeSequence[] newMeans = new FakeSequence[k];
			
			//for each new mean
			for(int i = 0; i < k; i++) {
				System.out.println("k" + (i+1));
				ArrayList<FakeSequence> cluster = clusters.get(i);
				System.out.println("cluster = " + cluster.size());
				
				//how long to make new FakeSequence? Average length, for now.
				int l = 0;
				for(FakeSequence s: cluster) {
					l += s.data.size();
				}
				l /= cluster.size(); //average length
				
				ArrayList<FakeSubpoint> newSeqPoints = new ArrayList<FakeSubpoint>(l);
				
				//for each FakeSubpoint
				//System.out.println("l = " + l);
				for(int j = 0; j < l; j++) {
					//System.out.println("j=" + j);
					String[] attrList = new String[numAttrs];
					//for each attribute
					for(int attr = 0; attr < numAttrs; attr++) {
						//categorical, so find the most common value and that is the value for the new mean
						
						//create frequency list for values
						HashMap<String, Integer> freqList = new HashMap<String, Integer>();
						for(FakeSequence s: cluster) {
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

					}//end for each attribute
					//System.out.println(Arrays.toString(attribute_labels));
					//System.out.println(Arrays.toString(attrList));
					FakeSubpoint sub = new FakeSubpoint(attrList);
					newSeqPoints.add(sub);
				}//end for each FakeSubpoint
				
				FakeSequence newMean = new FakeSequence(newSeqPoints);
				newMeans[i] = newMean;
				//print(newMean);
			}//end for each mean
			
			System.out.println(Arrays.toString(newMeans));
			//new Scanner(System.in).nextLine();
		} while (!clusters.equals(oldClusters));
		
		if(numIters > 1) {
			new Scanner(System.in).nextLine();
		}
		System.out.println("Final^^");
		BKMeans.pause();
		return means;
	}
	
	public static void print(FakeSequence seq) {
		for(FakeSubpoint sub: seq.data) {
			System.out.println(sub.toString());
		}
	}
	
	public static void testReflexivity(ArrayList<FakeSequence> data) {
		for(FakeSequence s: data) {
			if (s.distance(s) != 0) {
				System.out.println(s.toString());
				new Scanner(System.in).nextLine();
			}
		}
	}
	
	public static void testSymmetricity(ArrayList<FakeSequence> data) {
		for(FakeSequence s1: data) {
			for(FakeSequence s2: data) {
				if (s1.distance(s2) != s2.distance(s1)) {
					System.out.println(s1.toString());
					System.out.println(s2.toString());
					new Scanner(System.in).nextLine();
				}
			}
		}
	}
	
	public static void testTriangleEquality(ArrayList<FakeSequence> data) {
		//triangle inequality
		for (FakeSequence s1: data) {
			for (FakeSequence s2: data) {
				for (FakeSequence s3: data) {
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
	
	public static void testDist(ArrayList<FakeSequence> data) {
		testReflexivity(data);
		System.out.println("reflex good");
		testSymmetricity(data);
		System.out.println("symmet good");
		testTriangleEquality(data);
		System.out.println("tri good");

	}
	
	public static void main(String[] args) {
		ArrayList<FakeSequence> data = FakeTesting.makeData2("Fake-data4.csv");
		//testDist(data);
		
		while(true) {
			FakeSequence[] means = FakeTesting.kMedoids(data, 2);
		}
	}
}
