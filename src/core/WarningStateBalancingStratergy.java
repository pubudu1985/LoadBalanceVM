package core;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;

/**
 * @author Kavya
 * 
 */
public class WarningStateBalancingStratergy implements LoadBalancingStratergy {

	LoadBalancing balancing = new LoadBalancing();

	/*
	 * This method runs the strategy for warning state service instance
	 * 
	 * @param serviceIns- service instance which is warning state
	 */
	@Override
	public void runLoadBalancing(ServiceInstance si) {
		System.out.println("START - WarningStateBalancingStratergy:runLoadBalancing()");
		MigrateVM migrateVM= new MigrateVM();
		migrateVM.migrateVM(si);
		
		//check if optimal
		
		ResourcePool rsp = InformationCenter.getResourcePool(si);
		String rspStatus = rsp.getConfigStatus().toString();
		if (! rspStatus.equalsIgnoreCase("Green")){
			while(rspStatus.equalsIgnoreCase("Green")){
			migrateVM.migrateVM(si);
			}
		}
		
		System.out.println("END - WarningStateBalancingStratergy:runLoadBalancing()");

	}

}
