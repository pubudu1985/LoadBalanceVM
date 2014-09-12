/**
 * 
 */
package core;

import java.rmi.RemoteException;

import com.vmware.vim25.FileFault;
import com.vmware.vim25.InsufficientResourcesFault;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineQuestionInfo;
import com.vmware.vim25.VmConfigFault;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * @author Kavya
 * 
 */
public class MigrateVM {

	/**
	 * This method migrate the appropriate vm from the si
	 * 
	 * @param si
	 *            is the host which is not in optimal state
	 */
	public void migrateVM(ServiceInstance si) {
		System.out.println("START- MigrateVM : migrateVM() \n");
		LoadBalancing balancing = new LoadBalancing();
		HostSystem hostSystem = InformationCenter.getHostSystem(si);
		ServiceInstance siNew = balancing.getLessUtilizedHost();
		String siName = si.getServerConnection().getUrl().toString();
		String siNewName = siNew.getServerConnection().getUrl().toString();

		String vmName = balancing.getVMtoBeMigrated(si, siNew);
		System.out.println("Migrating "
				+ vmName + " from " + siName + " to " + siNewName);
		if (siNew != null && !siName.equals(siNewName) && vmName != null) {
			// migrate
			migrate(siNew, vmName);
			// set warning count to 0
			InformationCenter.warningStateCount.put(hostSystem
					.getServerConnection().getUsername().toString(), 0);
		} else {
			System.out
					.println("No optimal service instance and  virtual to migrate");
		}
		System.out.println("END- MigrateVM : migrateVM()");
	}

	private void migrate(ServiceInstance siNew, String vmName) {

		// Retrieve VM from VMList matching the passed name.

		VirtualMachine vm = InformationCenter.getVM(vmName);
		Folder vmFolder = InformationCenter.getVMFolder(siNew);
		String dsPath = getDSPath(vmName);
		ResourcePool rsp = InformationCenter.getResourcePool(siNew);
		HostSystem host = InformationCenter.getHostSystem(siNew);
		try {

			if (vm.getRuntime().getPowerState()
					.equals(VirtualMachinePowerState.poweredOn)) {
				Task suspendTask = vm.suspendVM_Task();

				// Wait for completion of register VM task before updating VM
				// list
				InformationCenter.waitForTaskCompletion(suspendTask);
			}
			// VM needs to be registered to a new host before power on.
			Task regTask = vmFolder.registerVM_Task(dsPath, vmName, false, rsp,
					host);

			// Delete the VM registered with previous host from the list.
			InformationCenter.deleteVMfromList(vmName);

			// Unregister VM from the previous host
			vm.unregisterVM();

			// Wait for completion of register VM task before updating VM list
			InformationCenter.waitForTaskCompletion(regTask);

			// Update the VM list to have VM registered with the new host and
			// retrieve the VM into VM instance.
			InformationCenter.updateVMList(siNew);
			vm = InformationCenter.getVM(vmName);

			// Once registered and retrieved, Power on the machine.
			Task tskVMOn = vm.powerOnVM_Task(host);

			// Sleep for 100 ms for task to get initiated.
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				System.out.println("Thread interrupted.");
				e1.printStackTrace();
			}

			/*
			 * When a machine is powered on first time after registering to a
			 * new host, a VM question is asked - to confirm if the machine is
			 * moved or copied. This question is asked when task is 95% complete
			 * and needs to be answered to PowerOn the machine. Thus we need to
			 * wait until the PowerOn task starts running and is 95% complete.
			 */
			while (tskVMOn.getTaskInfo().getState() != TaskInfoState.running
					|| tskVMOn.getTaskInfo().getProgress() < 95) {
				try {
					Thread.sleep(1000);
					if (tskVMOn.getTaskInfo().getState() != TaskInfoState.success) {
						break;
					}
				} catch (InterruptedException e) {
					System.out.println("Thread interrupted.");
					e.printStackTrace();
				}
			}

			/*
			 * After re-registering a VM, we need to answer the question if VM
			 * is moved or copied. For our purpose, it is moved. Choice option
			 * for move is 1. It can be changed to 2 for Copy.
			 */
			// Sleep for 500 ms for task to get initiated.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				System.out.println("Thread interrupted.");
				e1.printStackTrace();
			}
			VirtualMachineQuestionInfo ques = vm.getRuntime().getQuestion();
			String ch = "1";
			vm.answerVM(ques.getId(), ch);

		} catch (VmConfigFault e) {
			System.out.println("Invalid VM configuration");
			e.printStackTrace();
		} catch (TaskInProgress e) {
			System.out
					.println("This VM is currently running tasks on other hosts, Hence cannot be powered on.");
			e.printStackTrace();
		} catch (FileFault e) {
			System.out.println("VM file invalid");
			e.printStackTrace();
		} catch (InvalidState e) {
			System.out.println("VM not in a valid state to Power on.");
			e.printStackTrace();
		} catch (InsufficientResourcesFault e) {
			System.out
					.println("Host doesnot have sufficient resources to power on the VM");
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println();
		System.out.println("Virtualmachine " + vm.getName()
				+ "Migrated and PoweredOn successfully!!!");
		System.out.println();
		

	} // End of method registerPowerOn

	public String getDSPath(String name) {
		return "[nas]" + name + "/" + name + ".vmx";
	}
}
