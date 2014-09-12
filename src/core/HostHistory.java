/**
 * 
 */
package core;

import java.util.Date;
import java.util.Stack;

import com.vmware.vim25.mo.HostSystem;

/**
 * @author Kavya
 * 
 */
public class HostHistory {

	/**
	 * data variables to store history
	 */
	private Date time;
	private double predictedValue;
	private double actualValue;
	private int timeSeriesCount;

	/**
	 * static method which add history of hostSystem and called when hostSystem is created
	 * @param hostSystem 
	 */
	public static void addNewHistoryRecord1(HostSystem hostSystem) {

		HostHistory history = new HostHistory();
		history.setActualValue(20);
		history.setPredictedValue(25);
		history.setTimeSeriesCount(1);
		history.setTime(new Date());
		Stack<HostHistory> hostHistory = new Stack<HostHistory>();
		hostHistory.push(history);
		InformationCenter.hostHistoryMap.put(hostSystem.getServerConnection().getUrl().toString(), hostHistory);

	}

	/**
	 * static method which add history of hostSystem and called when hostSystem is created
	 * @param hostSystem 
	 */
	public static void addNewHistoryRecord2(HostSystem hostSystem) {

		HostHistory history = new HostHistory();
		history.setActualValue(30);
		history.setPredictedValue(35);
		history.setTimeSeriesCount(1);
		history.setTime(new Date());
		Stack<HostHistory> hostHistory =InformationCenter.hostHistoryMap
				.get(hostSystem.getServerConnection().getUrl().toString());
		hostHistory.push(history);
		InformationCenter.hostHistoryMap.put(hostSystem.getServerConnection().getUrl().toString(), hostHistory);

	}

	/**
	 * @param hostSystem - whose history needs to be updated
	 * @param actualMemUsage - the actual memory usage
	 */
	public static void updateHistoryRecord(HostSystem hostSystem, double actualMemUsage) {
		Stack<HostHistory> hostHistory = InformationCenter.hostHistoryMap
				.get(hostSystem.getServerConnection().getUrl().toString());
		HostHistory prevHostHistory = hostHistory.pop();
		prevHostHistory.setActualValue(actualMemUsage);
		hostHistory.push(prevHostHistory);
		InformationCenter.hostHistoryMap.put(hostSystem.getServerConnection().getUrl().toString(), hostHistory);

	}

	/**
	 * @param hostSystem - whose history needs to be added
	 * @param predictedMemUsage - the predicted memory usage
	 */
	public static void addHistoryRecord(HostSystem hostSystem, double predictedMemUsage) {

		Stack<HostHistory> hostHistory = InformationCenter.hostHistoryMap
				.get(hostSystem.getServerConnection().getUrl().toString());
		HostHistory prevHostHistory = hostHistory.pop();
		int count = prevHostHistory.getTimeSeriesCount() + 1;
		hostHistory.push(prevHostHistory);

		HostHistory history = new HostHistory();
		history.setPredictedValue(predictedMemUsage);
		history.setTimeSeriesCount(count);
		history.setTime(new Date());
		hostHistory.push(history);

		InformationCenter.hostHistoryMap.put(hostSystem.getServerConnection().getUrl().toString(), hostHistory);

	}

	/**
	 * @return the time
	 */
	public Date getTime() {
		return time;
	}

	/**
	 * @param time
	 *            the time to set
	 */
	public void setTime(Date time) {
		this.time = time;
	}

	/**
	 * @return the predictedValue
	 */
	public double getPredictedValue() {
		return predictedValue;
	}

	/**
	 * @param predictedValue
	 *            the predictedValue to set
	 */
	public void setPredictedValue(double predictedValue) {
		this.predictedValue = predictedValue;
	}

	/**
	 * @return the actualValue
	 */
	public double getActualValue() {
		return actualValue;
	}

	/**
	 * @param actualValue
	 *            the actualValue to set
	 */
	public void setActualValue(double actualValue) {
		this.actualValue = actualValue;
	}

	/**
	 * @return the timeSeriesCount
	 */
	public int getTimeSeriesCount() {
		return timeSeriesCount;
	}

	/**
	 * @param timeSeriesCount
	 *            the timeSeriesCount to set
	 */
	public void setTimeSeriesCount(int timeSeriesCount) {
		this.timeSeriesCount = timeSeriesCount;
	}

}
