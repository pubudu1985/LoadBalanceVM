/**
 * 
 */
package core;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * 
 * 
 */
public class InformationCenter {

	
	/**
	 * this list maintains information about all the hosts currently existing in
	 * our VI
	 */
	public static List<ServiceInstance> hostList = new ArrayList<ServiceInstance>();

	/**
	 * this map hold the history stack for each host
	 */
	public static Map<String, Stack<HostHistory>> hostHistoryMap = new HashMap<String, Stack<HostHistory>>();

	/**
	 * this map holds the warning state count for each host
	 */
	public static Map<String, Integer> warningStateCount = new HashMap<String, Integer>();

	/**
	 * this is the maximum number of times the host can go from optimal to
	 * warning when predicted
	 */
	public static final int WARNING_STATE_COUNT = 3;

	/**
	 * list holds the hosts present
	 */
	public static List<Host> hostSystemList = new ArrayList<Host>();

	/*
	 * This is a list of all the VMs.
	 */
	public static List<VirtualMachine> VMList = new ArrayList<VirtualMachine>();

	/**
	 * @return the hostList - all the hosts present in our VI
	 */
	public static List<ServiceInstance> getHostList() {
		List<ServiceInstance> instances = new ArrayList<ServiceInstance>();

		for (Host host : hostSystemList) {
			try {
				ServiceInstance instance = new ServiceInstance(new URL(
						host.getUrl()), host.getUserName(), host.getPassword());
				instances.add(instance);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

		}
		return instances;
	}

	/*
	 * This method returns the default host (First host). Default host is used
	 * while creation of VMs. If user does not specify any host, VM is created
	 * in the default host. Please note - VMs do not occupy resources until
	 * powered ON. Also, this method can be changed later to return host entered
	 * by user. If no host entered, then return default host
	 */
	public static ServiceInstance getHost() {
		return getHostList().get(0);
	}

	/*
	 * This methods waits for successful completion of the passed task.
	 */
	public static void waitForTaskCompletion(Task t) {
		// Sleep for 100 ms for task to get initiated.
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			System.out.println("Thread interrupted.");
			e1.printStackTrace();
		}

		// Loop until task gets into SUCCESS state.
		try {
			while (t.getTaskInfo().getState() != TaskInfoState.success) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					System.out.println("Thread interrupted.");
					e.printStackTrace();
				}
			}
		} catch (InvalidProperty e) {
			System.out.println("Invalid property passed");
			e.printStackTrace();
		} catch (RuntimeFault e) {
			System.out.println("Runtime error occured");
			e.printStackTrace();
		} catch (RemoteException e) {
			System.out.println("Remote exception occured");
			e.printStackTrace();
		}
	}

	/*
	 * This method returns the root folder of host service instance. Individual
	 * service instance has to be retrieved from the InformationCenter.
	 */
	public static Folder getRootFoler(ServiceInstance si) {
		return si.getRootFolder();
	}

	/*
	 * This method returns the root folder of host service instance. Individual
	 * service instance has to be retrieved from the InformationCenter.
	 */
	public static Folder getVMFolder(ServiceInstance si) {
		Folder rootFolder = si.getRootFolder();
		Datacenter dataCenter = null;
		try {
			dataCenter = (Datacenter) rootFolder.getChildEntity()[0];
		} catch (InvalidProperty e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		Folder vmf = null;
		try {
			vmf = dataCenter.getVmFolder();
		} catch (InvalidProperty e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return vmf;
	}

	/*
	 * This method returns the resource pool of host service instance.
	 * Individual service instance has to be retrieved from the
	 * InformationCenter.
	 */
	public static ResourcePool getResourcePool(ServiceInstance si) {
		Folder rootFolder = si.getRootFolder();
		ManagedEntity[] mesRsp = null;
		try {
			mesRsp = new InventoryNavigator(rootFolder)
					.searchManagedEntities("ResourcePool");
		} catch (InvalidProperty e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		ResourcePool rsp = (ResourcePool) mesRsp[0];
		return rsp;
	}

	/*
	 * This method returns the host system of host service instance. Individual
	 * service instance has to be retrieved from the InformationCenter.
	 */
	public static HostSystem getHostSystem(ServiceInstance si) {
		Folder rootFolder = si.getRootFolder();
		ManagedEntity[] mesHost = null;
		try {
			mesHost = new InventoryNavigator(rootFolder)
					.searchManagedEntities("HostSystem");
		} catch (InvalidProperty e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		HostSystem host = (HostSystem) mesHost[0];
		return host;
	}

	/*
	 * Get the list of VMs
	 */
	public static List<VirtualMachine> getVMList() {
		return VMList;
	}

	/*
	 * Get the name of all the POWER ON VMs in list
	 */
	public static ArrayList<String> getONVMNameList() {
		ArrayList<String> onVmName = new ArrayList<>();
		for (VirtualMachine vmi : VMList) {
			if (vmi.getRuntime().getPowerState()
					.equals(VirtualMachinePowerState.poweredOn))
				onVmName.add(vmi.getName());
		}
		// for (int i = 0; i<15; i++) {
		// onVmName.add("VM" + i);
		// }
		return onVmName;
	}

	/*
	 * Get the name of all the POWER OFF VMs in list
	 */
	public static ArrayList<String> getOFFVMNameList() {
		ArrayList<String> offVmName = new ArrayList<>();
		for (VirtualMachine vmi : VMList) {
			if (vmi.getRuntime().getPowerState()
					.equals(VirtualMachinePowerState.poweredOff))
				offVmName.add(vmi.getName());
		}
		// for (int i = 0; i<15; i++) {
		// offVmName.add("VM" + i);
		// }
		return offVmName;
	}

	/*
	 * Adding a VM to this list. Not sure if this method needs to be moved to VM
	 * class itself.
	 */
	public static void addVMtoList(VirtualMachine vm) {
		VMList.add(vm);
	}

	/*
	 * After a Vm has been unregistered, it needs to be removed from list.
	 */
	public static void deleteVMfromList(String name) {
		VirtualMachine vm = null;
		for (VirtualMachine vmi : VMList) {
			String vmName = vmi.getName();
			if (vmName.equals(name)) {
				vm = vmi;
			}
		}
		VMList.remove(vm);
	}

	/*
	 * This method is required when a new host is added and it already has some
	 * VMs. VMlist need to be updated to add any VMs that do not already exist
	 * in the list.
	 */
	public static void updateVMList(ServiceInstance si) {
		Folder root = si.getRootFolder();
		ManagedEntity[] mesVM = null;
		try {
			mesVM = new InventoryNavigator(root)
					.searchManagedEntities("VirtualMachine");
		} catch (InvalidProperty e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < mesVM.length; i++) {
			VirtualMachine vm = (VirtualMachine) mesVM[i];
			Boolean found = false;
			String name = vm.getName();
			for (VirtualMachine vmi : VMList) {
				String vmName = vmi.getName();
				if (vmName.equals(name)) {
					found = true;
					break;
				}
			}
			if (found == false) {
				addVMtoList(vm);
			}
		}
	}

	/*
	 * Returning a particular VM from the list. Might need to change this method
	 * to return a selected list of VMs. Needed in PowerOn/ PowerOff
	 */
	public static VirtualMachine getVM(String name) {
		/*
		 * If matching VM not found in the list, null will be returned This
		 * needs to be handled in all the methods calling this method.
		 */
		VirtualMachine vm = null;
		for (VirtualMachine vmi : VMList) {
			String vmName = vmi.getName();
			if (vmName.equals(name)) {
				vm = vmi;
			}
		}
		return vm;
	}

	/*
	 * To print the list of VMs
	 */
	public static void printVMList() {
		System.out.println("Number of VMs in list = " + VMList.size());
		for (VirtualMachine vmi : VMList) {
			System.out.println("Name: " + vmi.getName());
		}
	}

	/*
	 * Print list of VMs in a given host
	 */
	public static void printHostVM(ServiceInstance si) {
		Folder root = si.getRootFolder();
		ManagedEntity[] mesVM = null;
		try {
			mesVM = new InventoryNavigator(root)
					.searchManagedEntities("VirtualMachine");
		} catch (InvalidProperty e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println("Folder Name: " + mesVM[0].getParent().getName());
		for (int i = 0; i < mesVM.length; i++) {
			VirtualMachine vm = (VirtualMachine) mesVM[i];
			System.out.println("VM Name: " + vm.getName());
		}
	}

	/**
	 * @param instance 
	 * @return list of VMs present in instance
	 */
	public static List<VirtualMachine> getVMsOfHost(ServiceInstance instance) {
		List<VirtualMachine> vms = null;

		Folder rootFolder = instance.getRootFolder();
		try {
			ManagedEntity[] meVMs = new InventoryNavigator(rootFolder)
					.searchManagedEntities("VirtualMachine");

			if (meVMs.length > 0 && meVMs != null) {
				vms = new ArrayList<VirtualMachine>();
				for (ManagedEntity managedEntity : meVMs) {
					if (managedEntity != null) {
						VirtualMachine vm = (VirtualMachine) managedEntity;
						vms.add(vm);
					}
				}
			}
		} catch (InvalidProperty e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		return vms;
	}

	/**
	 * displays the hosts and it vms running on it
	 */
	public static void displayHostVms() {

		for (ServiceInstance si : InformationCenter.getHostList()) {
			System.out.println("Host -"
					+ si.getServerConnection().getUrl().toString());
			System.out.println("Virtual machines");
			List<VirtualMachine> vms = InformationCenter.getVMsOfHost(si);
			if (vms.size() > 0 && vms != null) {
				for (VirtualMachine vm : vms) {
					if (vm != null) {
						System.out.println(vm.getName());
					}
				}
			}
		}
	}

}
