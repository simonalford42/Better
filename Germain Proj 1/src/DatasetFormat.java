import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public enum DatasetFormat {
    FIRST ( //weights
    		new double[] {0,  			1,  		1,	 	1, 		1, 		 1, 		1, 			0, 				0},
    			//		IPV4_ADDR	URL_PATH	CATEGORY	NAME	NAME2	UXT_METHOD	UXT_STATUS	UXT_DURATION	<N/A>'
    			// 0 is for categorical data, 1 is for numerical,
	    	//attr types
    		new int[] {0, 0,0,0, 0,0,0, 1, 0},
	    	0, 2,
	    	new DateParser() {
	    		public Date parse(String s) {
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
	   		}
	    ),
    
    JUNE28 (
    		//weights
    		new double[] {0,  			1,  		4,	 	3, 		2, 		 1,     1,		1, 			1, 			0,           	0},
			//			"IPV4_ADDR","URL_PATH","CATEGORY","NAME","NAME2","NAME3","NAME4","UXT_METHOD","UXT_STATUS","UXT_DURATION","'<N/A>'"
    		// attrTypes
    		new int[] {0,			0,			0,		0, 		2,		2,		2, 		0,			0,			1, 				0},
    		0, 2, //timestamp index and userindex
    		new DateParser() {
    			public Date parse(String s) {
    				String apm = s.substring(s.length() - 3);
    				s = s.substring(0, s.length() - 9);
    				s = s + apm;
    				SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy hh.mm.ss.SSS a");
    				Date d = null;
    				
    				try {
    					d = format.parse(s);
    				} catch (ParseException e) {
    					e.printStackTrace();
    				}

    				return d;
    			}
    		}
    		);
	
	public static final int CAT_ATTR = 0;
	public static final int NUM_ATTR = 1;
	public static final int STR_MATCH_ATTR = 2;
	
    public final double[] weights;
    public final int[] attrTypes;
    public final int timestampIndex;
    public final int userIndex;
    public final DateParser dp; 
    public final int numAttrs;
    public final int numData;
    public final double maxDist;
    public final ArrayList<Integer> numIndeces;
    
    DatasetFormat(double[] weights, int[] attrTypes, int timestampIndex, int userIndex, DateParser dp) {
    	this.weights = weights;
    	this.attrTypes = attrTypes;
    	this.timestampIndex = timestampIndex;
    	this.userIndex = userIndex;
    	this.dp = dp;
    	this.numAttrs = attrTypes.length;
    	this.numData = numAttrs + 2;
    	numIndeces = new ArrayList<>();
    	for(int i = 0; i < numAttrs; i++) {
    		if (attrTypes[i] == NUM_ATTR)
    			numIndeces.add(i);
    	}
    	maxDist = Arrays.stream(weights).sum(); 
    }
}