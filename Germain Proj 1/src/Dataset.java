import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public enum Dataset {
    FIRST (
    		new double[] {0, 				0, 		  0,  			1,  		1,	 	1, 		1, 		 1, 		1, 			0, 				0},
    			//		UXT_START_TIME	IPV4_ADDR	USERNAME	URL_PATH	CATEGORY	NAME	NAME2	UXT_METHOD	UXT_STATUS	UXT_DURATION	<N/A>'
    			// 0 is for categorical data, 1 is for numerical,
	    	new int[] {3,0,4, 0,0,0, 0,0,0, 1, 0},
	    	new DateParser() {
	    		public Date makeDate(String s) {
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
    		new double[] {0, 		0, 		  0,  			1,  		4,	 	3, 		2, 		 1,     1,		1, 			1, 			0,           	0},
			//	"UXT_START_TIME","IPV4_ADDR","USERNAME","URL_PATH","CATEGORY","NAME","NAME2","NAME3","NAME4","UXT_METHOD","UXT_STATUS","UXT_DURATION","'<N/A>'"
			// 0 is for categorical data, 1 is for numerical, 2 string matching 3 timestamp, 4 user
    		new int[] {3,			0,			4, 			0,			0,		0, 		2,		2,		2, 		0,			0,			1, 				0},
    		new DateParser() {
    			public Date makeDate(String s) {
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
    		);

    public final double[] weights;
    public final int[] attrTypes;
    public final DateParser dp; 
    
    Dataset(double[] weights, int[] attrTypes, DateParser dp) {
    	this.weights = weights;
    	this.attrTypes = attrTypes;
    	this.dp = dp;
    }
}