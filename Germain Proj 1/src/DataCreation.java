import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class DataCreation {
	
	public static void createData2() {
		String s2 = "";
		String s = "";
		String a = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		String b = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";
		for(int j = 0; j < 11; j++) {
			s = a.substring(0, j) + b.substring(j, 10);
			s2 += s + "\n";
		}
		
		FileWriter fw;
		try {
			fw = new FileWriter("Fake-Data4.csv");
			fw.write(s2);
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void createData1() {
		
		double[][] mr1probs = {{0, 1, 0},     {0.1, 0.9, 0}, {0.2, 0.8, 0},
							   {0.3, 0.7, 0}, {0.4, 0.6, 0}, {0.5, 0.5, 0},
							   {0.6, 0.4, 0}, {0.7, 0.3, 0}, {0.8, 0.2, 0},
							   {0.9, 0.1, 0}, {1, 0, 0}};
		double[][] mr2probs = {{.33, .33, 0}, {.33, .33, 0}, {.33, .33, 0},
							   {.33, .33, 0}, {.33, .33, 0}, {.33, .33, 0},
							   {.33, .33, 0}, {.33, .33, 0}, {.33, .33, 0},
							   {.33, .33, 0}, {.33, .33, 0}};
		double[][] mr3probs = {{0, 0.1, 0.9}, {0, 0.1, 0.9}, {0, 0.1, 0.9},
							   {0, 0.1, 0.9}, {0, 0.1, 0.9}, {0, 0.1, 0.9},
							   {0, 0.1, 0.9}, {0, 0.1, 0.9}, {0, 0.1, 0.9},
							   {0, 0.1, 0.9}, {0, 0.1, 0.9}};
		double[][][] probs = {mr2probs, mr2probs, mr2probs};
		
		String file = "";
		Random rand = new Random();
		
		for(int type = 0; type < 3; type++) {
			for(int i = 0; i < 300; i++) {
				String uid = "mr" + type + "_" + i;
				String sub = "";
				for(int index = 0; index < 11; index++) {
					String timestamp = Integer.toString(type*50000 + i*100 + index);
					double r = rand.nextDouble();
					if (r < probs[type][index][0]) {
						sub = "A";
					} else if (r - probs[type][index][0] < probs[type][index][1]) {
						sub = "B";
					} else {
						sub = "C";
					}
					String line = uid + "," + timestamp + "," + sub + "\n";
					file += line;
				}
			}
		}
		
		try {
			FileWriter fw = new FileWriter("Fake-data2.csv");
			fw.write(file);
			fw.close();
		} catch (Exception e) {
			System.out.println("bad");
			System.exit(0);
		}
	}
	
	public static void main(String[] args) {
		createData2();
	}
}
