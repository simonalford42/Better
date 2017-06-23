import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PatternFinder {
	Simpler s;
	
	public PatternFinder(Simpler s) {
		this.s = s;
	}
	
	public static String[] findMostCommon(List<String> seqs, int l, int n) {
		int b = n*n;
		
		String[] seqcans = new String[n];
		for(int i = 0; i < n; i++) {
			seqcans[i] = "";
		}
		int[] weights = new int[n];
		int[] counts = new int[n];
		
		for(String seq: seqs) {
			if (seq.length() < l)
				continue;
			String subseq = seq.substring(0, l);
			for(int i = 0; i < seq.length() - l; i++) {
				boolean done = false;
				boolean[] seqsEqual = new boolean[n];
				boolean[] wEquals0 = new boolean[b];
				for(int j = 0; j < n; j++) {
					seqsEqual[j] = seqcans[j].equals(subseq);
					wEquals0[j] = weights[j] == 0;
				}
				
				for(int j = 0; j < n; j++) {
					if (!wEquals0[j] && seqsEqual[j]) {
						weights[j] += b;
						counts[j] += 1;
						done = true;
						break;
					}
				}
				
				if (!done) {
					if (Arrays.stream(weights).allMatch(e -> e != 0) && IntStream.range(0,n)
                            .mapToObj(idx -> seqsEqual[idx]).noneMatch(e -> e)) {
						int min = Integer.MAX_VALUE;
						int index = -1;
						for(int k = 0; k < n; k++) {
							if (weights[k] < min) {
								min = weights[k];
								index = k;
							}
						}
						weights[index] = weights[index] - 1;
					} else {
						for(int j = 0; j < n; j++) {
							if (weights[j] == 0) {
								weights[j] = b;
								counts[j] = 1;
								seqcans[j] = subseq;
								break;
							}
						}
					}
				}
			
				subseq = subseq.substring(1) + seq.charAt(l + i - 1);
			}
		}
		
		List<String> stringList = Arrays.asList(seqcans);
		Collections.sort(stringList, (left, right) -> counts[stringList.indexOf(left)] - counts[stringList.indexOf(right)]);
		return seqcans;
	}
	
	public static String generateData() {
		StringBuilder data = new StringBuilder(1000000);
		String[] vals = {"B", "A"};
		Random rand = new Random();
		for(int i = 0; i < 1000000; i++) {
			if (rand.nextInt(10) < 3) {
				data.append(vals[0]);
			} else {
				data.append(vals[1]);
			}
		}
		
		return data.toString();
	}
	
	public static void main(String[] args) {
		for(int i = 0; i < 10; i++) {
			String data = generateData();
			List<String> data2 = new ArrayList<>();
			data2.add(data);
			String[] common = findMostCommon(data2, 3, 5);
			for(String s: common) {
				System.out.println(s);
			}
		}
	}
}
