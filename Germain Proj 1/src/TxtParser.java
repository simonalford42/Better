import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;


public class TxtParser {
	public static ArrayList<String> parseFile(String filePath) {
		ArrayList<String> lines = new ArrayList<String>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));
			String line;
			while((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return lines;
	}
}
