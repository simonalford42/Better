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


public class FirstKMeans {
	public static String[] attribute_labels;
	public static ArrayList<OldSequence> data;
	public static double largestDuration;
	
	
	public static ArrayList<OldSequence> makeData(String filePath) {
		ArrayList<String> list = TxtParser.parseFile(filePath);
		attribute_labels = list.remove(0).split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

		ArrayList<OldSubpoint> list2 = new ArrayList<OldSubpoint>(list.size());
		for (String s: list) {
			OldSubpoint sp = new OldSubpoint(s);
			list2.add(sp);
			//all subpoints have data length 10 (10 attributes)
		}
		
		largestDuration = 0;
		for(OldSubpoint sp: list2) {
			try {
				if (Double.valueOf(sp.data[8]) > largestDuration) {
					largestDuration = Double.valueOf(sp.data[8]);
				}
			} catch (Exception e) {
				
			}
		}
		
		//now cluster by source address = s.data[1]
		LinkedHashMap<String, ArrayList<OldSubpoint>> sacs = new LinkedHashMap<String, ArrayList<OldSubpoint>>();
		for (OldSubpoint s: list2) {
			//System.out.println(s.data[0]);
			if (sacs.containsKey(s.data[1])) {
				sacs.get(s.data[1]).add(s);
			} else {
				ArrayList<OldSubpoint> a = new ArrayList<OldSubpoint>();
				a.add(s);
				sacs.put(s.data[1], a);
			}
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
		
		//81 different clusters = sequences. half of these have length 1. One has length 234.
		
		//now we have to order each cluster's data into a sequence. 
		ArrayList<OldSequence> data = new ArrayList<OldSequence>(sacs.size());
		for(String key: sacs.keySet()) {
			ArrayList<OldSubpoint> cluster = sacs.get(key);
			Collections.sort(cluster);
			data.add(new OldSequence(cluster));
			
		}
		//now the clusters are /sequences/ !
	
		
		FirstKMeans.data = data;
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
	
	/**
	 * Instead of finding average thing, it chooses from options the one which minimizes total distance.
	 * @param data
	 * @param k
	 * @return
	 */
	public static OldSequence[] kMedoids(ArrayList<OldSequence> data, int k) {
		//System.out.println("k-medoids");
		int numIters = -1;
		
		int numAttrs = data.get(0).data.get(0).data.length;
		
		//initialize the k means to random sequences
		OldSequence[] means = new OldSequence[k];
		Random rand = new Random();
		ArrayList<Integer> used = new ArrayList<Integer>();
		int n = 0;
		//System.out.println("initial medoids:");
		for (int i = 0; i < k; i++) {
			do {
				n = rand.nextInt(data.size());
			} while (used.contains(n));
			
			means[i] = data.get(n);
			//print(means[i]);
			used.add(n);
		}
		
		
		OldSequence[] oldMeans = new OldSequence[k];
		ArrayList<ArrayList<OldSequence>> clusters = null;
		ArrayList<ArrayList<OldSequence>> oldClusters;
		
		do { //repeat until convergence
			numIters++;
			//System.out.println("new round");
			
			oldClusters = clusters;
			clusters = new ArrayList<ArrayList<OldSequence>>(k);
			for (int i = 0; i < k; i++) {
				clusters.add(new ArrayList<OldSequence>());
			}
			
			// associate each sequence with its nearest mean
			for (OldSequence s: data) {
				double minDistance = Integer.MAX_VALUE;
				int nearest = 0;
				for (int i = 0; i < k; i++) {
					OldSequence mean = means[i];
					double dist = s.distance(mean);
					if (dist < minDistance) {
						minDistance = dist;
						nearest = i;
					}
				}
				clusters.get(nearest).add(s);
			}
			
			// update means by averaging its associates
			
			//use sequence in data that minimizes total distance (k-medoids)
			OldSequence[] newMeans = new OldSequence[k];
			int[] compareTypes = {0, 	0,			0,	0, 	0, 		0, 		0, 			0, 		1, 		0};	
			
			//for each cluster
			for(int i = 0; i < k; i++) {
				//System.out.println("k" + (i+1));
				ArrayList<OldSequence> cluster = clusters.get(i);
				//System.out.println("cluster = " + cluster.size());
				
				double[] totalDistances = new double[cluster.size()];
				double minDist = Integer.MAX_VALUE;
				OldSequence min = cluster.get(0);
				for(int j = 0; j < cluster.size(); j++) {
					double dist = 0;
					OldSequence fs = cluster.get(j);
					//System.out.println(fs.toString());
					for(OldSequence fs2: cluster) {
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
				//print(min);
				//System.out.println();
			}//end for each mean
			
			//System.out.println("End round\n");
			means = newMeans;
			//new Scanner(System.in).nextLine();
		} while (!clusters.equals(oldClusters));
		
		//System.out.println("convergence");
		int[] ids = new int[k];
		
		for(int i = 0; i < k; i++) {
			//print(means[i]);
			ids[i] = means[i].id;
		}
		
		return means;
	}
	
	/**
	 * Returns the cluster means from the k means algorithm.
	 * @param data
	 * @param k
	 * @return
	 */
	public static OldSequence[] kMeans(ArrayList<OldSequence> data, int k) {
		System.out.println("k-means");
		int numIters = -1;
		
		int numAttrs = data.get(0).data.get(0).data.length;
		
		//initialize the k means to random sequences
		OldSequence[] means = new OldSequence[k];
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
		
		
		OldSequence[] oldMeans = new OldSequence[k];
		ArrayList<ArrayList<OldSequence>> clusters = null;
		ArrayList<ArrayList<OldSequence>> oldClusters;
		
		do { //repeat until convergence
			numIters++;
			System.out.println("new round");
			
			oldClusters = clusters;
			clusters = new ArrayList<ArrayList<OldSequence>>(k);
			for (int i = 0; i < k; i++) {
				clusters.add(new ArrayList<OldSequence>());
			}
			
			// associate each sequence with its nearest mean
			for (OldSequence s: data) {
				double minDistance = Integer.MAX_VALUE;
				int nearest = 0;
				for (int i = 0; i < k; i++) {
					OldSequence mean = means[i];
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
			// alternative is to use sequence in data that minimizes total distance (k-medoids)
			// think about: average of oppositely facing zigzags is a straight line.
			OldSequence[] newMeans = new OldSequence[k];
			int[] compareTypes = {0, 	0,			0,	0, 	0, 		0, 		0, 			0, 		1, 		0};	
			
			//for each new mean
			for(int i = 0; i < k; i++) {
				System.out.println("k" + (i+1));
				ArrayList<OldSequence> cluster = clusters.get(i);
				System.out.println("cluster = " + cluster.size());
				
				//how long to make new sequence? Average length, for now.
				int l = 0;
				for(OldSequence s: cluster) {
					l += s.data.size();
				}
				l /= cluster.size(); //average length
				
				ArrayList<OldSubpoint> newSeqPoints = new ArrayList<OldSubpoint>(l);
				
				//for each subpoint
				//System.out.println("l = " + l);
				for(int j = 0; j < l; j++) {
					//System.out.println("j=" + j);
					String[] attrList = new String[numAttrs];
					//for each attribute
					for(int attr = 0; attr < numAttrs; attr++) {
						switch(compareTypes[attr]) {
						case 0:
							//categorical, so find the most common value and that is the value for the new mean
							
							//create frequency list for values
							HashMap<String, Integer> freqList = new HashMap<String, Integer>();
							for(OldSequence s: cluster) {
								
								/**
								 * Only averaging sequences >= the average size for the farther out ones. Is this good?
								 */
								if (s.length <= j)
									continue;
								//if (s.data.get(j).isEmpty)
									//continue;
								
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
							for(OldSequence s: cluster) {
								
								if (s.length <= j) 
									continue;
								//if (s.data.get(j).isEmpty)
									//continue;
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
					//System.out.println(Arrays.toString(attribute_labels));
					//System.out.println(Arrays.toString(attrList));
					OldSubpoint sub = new OldSubpoint(attrList);
					newSeqPoints.add(sub);
				}//end for each subpoint
				
				OldSequence newMean = new OldSequence(newSeqPoints);
				newMeans[i] = newMean;
				print(newMean);
				System.out.println();
				BKMeans.pause();
			}//end for each mean
			
			means = newMeans;
			System.out.println("round over\n");
			//new Scanner(System.in).nextLine();
		} while (!clusters.equals(oldClusters));
		
		return means;
	}
	
	public static void print(OldSequence seq) {
		System.out.println("id " + seq.id + " length " + seq.length);
//		for(Subpoint sub: seq.data) {
//			System.out.println(sub.toString());
//		}
	}
	
	public static void pause() {
		new Scanner(System.in).nextLine();
	}
	
	public static void testReflexivity(ArrayList<OldSequence> data) {
		for(OldSequence s: data) {
			if (s.distance(s) != 0) {
				System.out.println(s.toString());
				new Scanner(System.in).nextLine();
			}
		}
	}
	
	public static void testSymmetricity(ArrayList<OldSequence> data) {
		for(OldSequence s1: data) {
			for(OldSequence s2: data) {
				if (s1.distance(s2) != s2.distance(s1)) {
					System.out.println(s1.toString());
					System.out.println(s2.toString());
					new Scanner(System.in).nextLine();
				}
			}
		}
	}
	
	public static void testTriangleEquality(ArrayList<OldSequence> data) {
		//triangle inequality
		for (OldSequence s1: data) {
			for (OldSequence s2: data) {
				for (OldSequence s3: data) {
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
	
	public static void testDist(ArrayList<OldSequence> data) {
		testReflexivity(data);
		System.out.println("reflex good");
		testSymmetricity(data);
		System.out.println("symmet good");
		testTriangleEquality(data);
		System.out.println("tri good");

	}
	
	public static OldSequence[] repeatMedoids(ArrayList<OldSequence> data, int k, int numIters) {
		HashMap<OldSequence, Integer> freqMap = new HashMap<OldSequence, Integer>();
		
		for(int i = 0; i < numIters; i++) {
			OldSequence[] means = kMedoids(data, k);
			for(OldSequence mean: means) {
				freqMap.put(mean, 1 + (freqMap.containsKey(mean) ? freqMap.get(mean) : 0));
			}
		}
		
		OldSequence[] means = new OldSequence[k];
		for(int i = 0; i < k; i++) {
			OldSequence mostCommon = null;
			int freq = 0;
			for(OldSequence s: freqMap.keySet()) {
				if (freqMap.get(s) > freq) {
					freq = freqMap.get(s);
					mostCommon = s;
				}
			}
			freqMap.keySet().remove(mostCommon);
			means[i] = mostCommon;
		}
		return means;
	}
	
	public static void medoidFreqTesting() {
		ArrayList<OldSequence> data = FirstKMeans.makeData("Simon-Data.csv");
				HashMap<Integer, Integer> idFreq = new HashMap<Integer, Integer>();
				for(int i = 0; i < 100; i++) {
					OldSequence[] means = repeatMedoids(data, 3, 5);
					for(OldSequence mean: means) {
						int id = mean.id;
						//System.out.println(id);
						if (idFreq.containsKey(id)) {
							idFreq.put(id, idFreq.get(id) + 1);
						} else {
							idFreq.put(id, 1);
							System.out.println(id + " " + mean.length);
						}
					}
				}
				System.out.println(idFreq.toString());
	}
	
	public static void main(String[] args) {
		ArrayList<OldSequence> data = FirstKMeans.makeData("Simon-Data.csv");
		
		testDist(data);
	}
}
