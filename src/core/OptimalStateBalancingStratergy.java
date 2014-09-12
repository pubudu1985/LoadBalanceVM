/**
 * 
 */
package core;

import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ServiceInstance;

/**
 * @author Kavya
 * 
 */
public class OptimalStateBalancingStratergy implements LoadBalancingStratergy {

	/* 
	 * This method runs the strategy for optimal service instance
	 * @param serviceIns- service instance which is in optimal state now
	 */
	@Override
	public void runLoadBalancing(ServiceInstance serviceIns) {
		System.out.println("START - OptimalStateBalancingStratergy:runLoadBalancing()");
		SESPredictionAlgo predictionAlgo = new SESPredictionAlgo();
		HostSystem hostSystem = InformationCenter.getHostSystem(serviceIns);
		// predict the host load for next T
		double predictedVal = predictionAlgo.predictValue(hostSystem);
		HostHistory.addHistoryRecord(InformationCenter.getHostSystem(serviceIns), predictedVal);
		// if its going in warning window , increase k by 1
		if (predictedVal > 60) {
			int count = InformationCenter.warningStateCount.get(hostSystem.getServerConnection().getUrl().toString());
			// increase count
			count = count + 1;
			InformationCenter.warningStateCount.put(hostSystem.getServerConnection().getUrl().toString(), count);
		}
		
		// if warning count is > k
		if (InformationCenter.warningStateCount.get(hostSystem.getServerConnection().getUrl().toString()) > InformationCenter.WARNING_STATE_COUNT) {
			// migrate
			MigrateVM migrateVM = new MigrateVM();
			migrateVM.migrateVM(serviceIns);
			InformationCenter.warningStateCount.put(hostSystem.getServerConnection().getUrl().toString(), 0);
			
		}

		System.out.println("END - OptimalStateBalancingStratergy:runLoadBalancing()");
	}

}
