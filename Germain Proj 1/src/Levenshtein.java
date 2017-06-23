import java.util.Scanner;

public class Levenshtein {
	
	public static double softLevi(SimpleSeq root, SimpleSeq query, double shiftPenalty) {
		double arr[][] = new double[root.length+2][query.length+2];
		
		for(int i = 2; i < root.length + 2; i++) {
			arr[i][1] = shiftPenalty*(i-1);
		}

		for(int i = 2; i < query.length + 2; i++) {
			arr[1][i] = shiftPenalty*(i-1);
		}

		for(int i = 2; i < root.length + 2; i++) {
			for(int j = 2; j < query.length + 2; j++) {
				double diff = Simpler.distance(query.data.get(j-2), root.data.get(i-2));
				arr[i][j] = min((arr[i-1][j] + shiftPenalty), (arr[i][j-1] + shiftPenalty), (arr[i-1][j-1] + diff));
			}
		}

  		return arr[root.length + 1][query.length + 1];
	}
	
	public static double softLevi(Sequence root, Sequence query, double shiftPenalty) {
		double arr[][] = new double[root.length+2][query.length+2];
		
		for(int i = 2; i < root.length + 2; i++) {
			arr[i][1] = shiftPenalty*(i-1);
		}

		for(int i = 2; i < query.length + 2; i++) {
			arr[1][i] = shiftPenalty*(i-1);
		}

		for(int i = 2; i < root.length + 2; i++) {
			for(int j = 2; j < query.length + 2; j++) {
				double diff = query.data.get(j-2).distance(root.data.get(i-2));
				arr[i][j] = min((arr[i-1][j] + shiftPenalty), (arr[i][j-1] + shiftPenalty), (arr[i-1][j-1] + diff));
			}
		}

  		return arr[root.length + 1][query.length + 1];
	}
	
	public static int softLevinshtein(int[] root, int[] query, int shiftPenalty) {
		int arr[][] = new int[root.length+2][query.length+2];
		
		for(int i = 2; i < root.length + 2; i++) {
			arr[i][0] = root[i-2];
			arr[i][1] = shiftPenalty*(i-1);
		}

		for(int i = 2; i < query.length + 2; i++) {
			arr[0][i] = query[i-2];
			arr[1][i] = shiftPenalty*(i-1);
		}

		for(int i = 2; i < root.length + 2; i++) {
			for(int j = 2; j < query.length + 2; j++) {
				int diff=0;
				if(arr[0][j] != arr[i][0])
					diff = Math.abs(arr[0][j] - arr[i][0]);
				arr[i][j] = (int)min((arr[i-1][j] + shiftPenalty), (arr[i][j-1] + shiftPenalty), (arr[i-1][j-1] + diff));
			}
		}

  		return arr[root.length+ 1][query.length + 1];
	}
	
	public static int hardLevinshtein(Sequence root, Sequence query, int shiftPenalty) {
		int arr[][] = new int[root.length+2][query.length+2];
		
		for(int i = 2; i < root.length + 2; i++) {
			arr[i][1] = (i-1);
		}

		for(int i = 2; i < query.length + 2; i++) {
			arr[1][i] = (i-1);
		}

		for(int i = 2; i < root.length + 2; i++) {
			for(int j = 2; j < query.length + 2; j++) {
				int diff = 0;
				if(query.data.get(j-2).distance(root.data.get(i-2)) == 0)
					diff = 1;
				arr[i][j] = (int)min((arr[i-1][j] + shiftPenalty), (arr[i][j-1] + shiftPenalty), (arr[i-1][j-1] + diff));
			}
		}

  		return arr[root.length + 1][query.length + 1];
	}
	
	public static int hardLevinshtein(SimpleSeq root, SimpleSeq query, int shiftPenalty) {
		int arr[][] = new int[root.length+2][query.length+2];
		
		for(int i = 2; i < root.length + 2; i++) {
			arr[i][1] = (i-1);
		}

		for(int i = 2; i < query.length + 2; i++) {
			arr[1][i] = (i-1);
		}

		for(int i = 2; i < root.length + 2; i++) {
			for(int j = 2; j < query.length + 2; j++) {
				int diff = 0;
				if(Simpler.distance(query.data.get(j-2), root.data.get(i-2)) == 0)
					diff = 2;
				arr[i][j] = (int)min((arr[i-1][j] + shiftPenalty), (arr[i][j-1] + shiftPenalty), (arr[i-1][j-1] + diff));
			}
		}

  		return arr[root.length + 1][query.length + 1];
	}
	
	public static int dtw(int[] root, int[] query, int shiftPenalty) {
		int arr[][] = new int[root.length+2][query.length+2];
		
		for(int i = 2; i < root.length + 2; i++) {
			arr[i][0] = root[i-2];
			arr[i][1] = (i-1);
		}

		for(int i = 2; i < query.length + 2; i++) {
			arr[0][i] = query[i-2];
			arr[1][i] = (i-1);
		}

		for(int i = 2; i < root.length + 2; i++) {
			for(int j = 2; j < query.length + 2; j++) {
				int diff = 0;
				if(arr[0][j] != arr[i][0])
					diff = 1;
				arr[i][j] = diff + (int)min(arr[i-1][j] + shiftPenalty, arr[i][j-1] + shiftPenalty, arr[i-1][j-1]);
			}
		}

  		return arr[root.length+ 1][query.length + 1];
	}
	
	public static int hardLevinshtein(int[] root, int[] query, int shiftPenalty) {
		int arr[][] = new int[root.length+2][query.length+2];
		
		for(int i = 2; i < root.length + 2; i++) {
			arr[i][0] = root[i-2];
			arr[i][1] = (i-1);
		}

		for(int i = 2; i < query.length + 2; i++) {
			arr[0][i] = query[i-2];
			arr[1][i] = (i-1);
		}

		for(int i = 2; i < root.length + 2; i++) {
			for(int j = 2; j < query.length + 2; j++) {
				int diff = 0;
				if(arr[0][j] != arr[i][0])
					diff = 2;
				arr[i][j] = (int)min((arr[i-1][j] + shiftPenalty), (arr[i][j-1] + shiftPenalty), (arr[i-1][j-1] + diff));
			}
		}

  		return arr[root.length+ 1][query.length + 1];
	}

	public static double min(double n1, double n2, double n3) {
		return Math.min(n1, Math.min(n2, n3));
	}
	
	public static int[] toIntArray(String s) {
		int[] a = new int[s.length()];
		for(int i = 0; i < s.length(); i++) {
			a[i] = Integer.valueOf(s.substring(i, i+1));
		}
		return a;
	}
	
	public static void main(String[] args) {
		while(true) {
			Scanner s = new Scanner(System.in);
			int[] s1 = toIntArray(s.nextLine());
			int[] s2 = toIntArray(s.nextLine());
			System.out.println(dtw(s1, s2, 1));
			System.out.println(hardLevinshtein(s1, s2, 1));
		}
	}
}