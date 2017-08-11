package eu.trentorise.smartcampus.mobility.dev.paths;

import java.util.ArrayList;
import java.util.List;

public class PolylineUtils {

	private static StringBuffer encodeNumber(int num) {
		StringBuffer encodeString = new StringBuffer();
		while (num >= 0x20) {
			int nextValue = (0x20 | (num & 0x1f)) + 63;
			encodeString.append((char) (nextValue));
			num >>= 5;
		}
		num += 63;
		encodeString.append((char) (num));
		return encodeString;
	}
	
	private static StringBuffer encodeSignedNumber(int num) {
        int sgn_num = num << 1;
        if (num < 0) {
            sgn_num = ~(sgn_num);
        }
        return(encodeNumber(sgn_num));
    }	

	/**
	 * Decode a polyline encoded with Google polyline encoding method
	 * 
	 * @param encoded
	 *            a polyline
	 * @return decoded array of coordinates
	 */
	public static List<double[]> decode(String encoded) {
		List<double[]> poly = new ArrayList<double[]>();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;

		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;
			poly.add(new double[] { lat / 1E5, lng / 1E5 });
		}

		return poly;
	}
	
    public static String encode(List<double[]> list) {
        StringBuffer encodedPoints = new StringBuffer();
        int prev_lat = 0, prev_lng = 0;
        for (double[] trackpoint: list) {
                int lat = (int)(trackpoint[0]*1E5);
                int lng = (int)(trackpoint[1]*1E5);
                encodedPoints.append(encodeSignedNumber(lat - prev_lat));
                encodedPoints.append(encodeSignedNumber(lng - prev_lng));                       
                prev_lat = lat;
                prev_lng = lng;
        }
        return encodedPoints.toString();
}	
	
}
