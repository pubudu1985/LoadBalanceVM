/**
 * 
 */
package core;

import java.io.IOException;
import java.rmi.RemoteException;

import com.vmware.vim25.InvalidState;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * @author Kavya
 * 
 */
public class OverLoadedStateBalancingStratergy implements
		LoadBalancingStratergy {

	/*
	 * This method runs the strategy for overloaded service instance
	 * 
	 * @param serviceIns- service instance which is overloaded
	 */
	@Override
	public void runLoadBalancing(ServiceInstance si) {
		// lock the host
		System.out
				.println("START - OverLoadedStateBalancingStratergy:runLoadBalancing()");
		LoadBalancing balancing = new LoadBalancing();
		ServiceInstance siNew = balancing.getLessUtilizedHost();
		String vmToMigrate = balancing.getVMtoBeMigrated(si, siNew);
		MigrateVM migrateVM = new MigrateVM();
		if (vmToMigrate != null) {
			// find optimal host

			if (siNew != null) {
				try {
					VirtualMachine vm = InformationCenter.getVM(vmToMigrate);

					Task task = vm.suspendVM_Task();
					InformationCenter.waitForTaskCompletion(task);
					try {
						if (balancing.checkOverloadedState(si)
								|| balancing.checkWarningState(si)) {
							while (!balancing.checkOptimalState(si)) {
								migrateVM.migrateVM(si);
							}
							System.out.println("Host "
									+ si.getServerConnection().getUrl()
											.toString() + " is Optimal");
							Task powerOnTask = vm
									.powerOnVM_Task(InformationCenter
											.getHostSystem(si));
							InformationCenter
									.waitForTaskCompletion(powerOnTask);
							System.out.println("Powered on the suspended task");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

					// check if optimal
				} catch (TaskInProgress e) {
					e.printStackTrace();
				} catch (InvalidState e) {
					e.printStackTrace();
				} catch (RuntimeFault e) {
					e.printStackTrace();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("No Optimal host to migrate");
			}

		}

		System.out
				.println("END - OverLoadedStateBalancingStratergy:runLoadBalancing()");
	}

}
