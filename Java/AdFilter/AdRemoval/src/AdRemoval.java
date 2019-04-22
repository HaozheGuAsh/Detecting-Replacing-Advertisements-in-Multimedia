import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdRemoval {
	private double getShannonEntropy(BufferedImage actualImage){
		 List<String> values= new ArrayList<String>();
		             int n = 0;
		             HashMap<Integer, Integer> occ = new HashMap<Integer,Integer>();
		             for(int i=0;i<actualImage.getHeight();i++){
		                 for(int j=0;j<actualImage.getWidth();j++){
		                   int pixel = actualImage.getRGB(j, i);
		                   int alpha = (pixel >> 24) & 0xff;
		                   int red = (pixel >> 16) & 0xff;
		                   int green = (pixel >> 8) & 0xff;
		                   int blue = (pixel) & 0xff;

		                   int d= (int)Math.round(0.2989 * red + 0.5870 * green + 0.1140 * blue);
		                  if(!values.contains(String.valueOf(d)))
		                      values.add(String.valueOf(d));
		                  if (occ.containsKey(d)) {
		                      occ.put(d, occ.get(d) + 1);
		                  } else {
		                      occ.put(d, 1);
		                  }
		                  ++n;
		           }
		        }
		        double e = 0.0;
		        for (HashMap.Entry<Integer, Integer> entry : occ.entrySet()) {
		             int cx = entry.getKey();
		             double p = (double) entry.getValue() / n;
		             e += p * (Math.log(p)/Math.log(2));
		        }
		 return -e;
	}
}
