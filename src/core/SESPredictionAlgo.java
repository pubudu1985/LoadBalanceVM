/**
 * 
 */
package core;

import java.util.Stack;

import com.vmware.vim25.mo.HostSystem;

/**
 * @author Kavya
 * 
 */
public class SESPredictionAlgo {

	/**
	 * alpha is smoothing constant
	 */
	private static final double alpha = 0.4;

	/**
	 * It employs prediction techniques based on Single Exponential Smoothing
	 * (SES) algorithm which is a kind of weighted moving average sequence data
	 * process method to judge whether Hosts will overload
	 * 
	 * @param host - host whose memory usage needs to be predicted
	 * @return predicted memory usage value
	 */
	public double predictValue(HostSystem host) {
		System.out.println("START - SESPredictionAlgo:predictValue()");
		double predictedValue = 0.0;
		Stack<HostHistory> history = InformationCenter.hostHistoryMap.get(host
				.getServerConnection().getUrl().toString());
		int t;
		HostHistory latestHistory = new HostHistory();
		latestHistory = history.pop();
		t = latestHistory.getTimeSeriesCount() + 1;
		double previousTimeActualVal = 0;
		double previousPredictedVal = 0;
		for (HostHistory hostHistory : history) {
			if (hostHistory.getTimeSeriesCount() == t - 1) {
				previousTimeActualVal = hostHistory.getActualValue();
				previousPredictedVal = hostHistory.getPredictedValue();
			}
		}
		predictedValue = previousPredictedVal + alpha
				* (previousTimeActualVal - previousPredictedVal);
		System.out.println("END - SESPredictionAlgo:predictValue()");
		return predictedValue;
	}

}
