/**
 * 
 */
package core;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import com.vmware.vim25.HostHardwareSummary;
import com.vmware.vim25.HostListSummary;
import com.vmware.vim25.HostListSummaryQuickStats;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineQuickStats;
import com.vmware.vim25.VirtualMachineSummary;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * @author Kavya
 * 
 */
public class LoadBalancing {

	/**
	 * this method runs the algorithm to balance virtual machine load on
	 * multiple hosts
	 * 
	 * @throws IOException
	 */
	public void balanceLoad() throws IOException {

		System.out.println("START - LoadBalancing:balanceLoad()");
		LoadBalancingStratergy balancingStratergy;

		List<ServiceInstance> siList = InformationCenter.getHostList();
		for (ServiceInstance serviceIns : siList) {
			if (serviceIns != null) {
				HostHistory.updateHistoryRecord(
						InformationCenter.getHostSystem(serviceIns),
						getMemoryUsage(serviceIns));
				if (checkOptimalState(serviceIns)) {
					System.out.println("host "
							+ serviceIns.getServerConnection().getUrl()
									.toString() + " in Optimal");
					balancingStratergy = new OptimalStateBalancingStratergy();
					balancingStratergy.runLoadBalancing(serviceIns);
				} else if (checkWarningState(serviceIns)) {
					System.out.println("host "
							+ serviceIns.getServerConnection().getUrl()
									.toString() + "  in Warning");
					balancingStratergy = new WarningStateBalancingStratergy();
					balancingStratergy.runLoadBalancing(serviceIns);
					
				} else if (checkOverloadedState(serviceIns)) {
					System.out.println("host "
							+ serviceIns.getServerConnection().getUrl()
									.toString() + " Overloaded");
					balancingStratergy = new OverLoadedStateBalancingStratergy();
					balancingStratergy.runLoadBalancing(serviceIns);
				}

			}

		}
		System.out.println("END - LoadBalancing:balanceLoad()");
		JOptionPane.showMessageDialog(null,
				"Loadbalancing completed. Check hosts status");
	}

	/**
	 * @return Map of host and its status
	 * @throws IOException
	 */
	public Map<String, String> getHostAndStatus() throws IOException {

		Map<String, String> hostStateMap = new HashMap<String, String>();

		for (ServiceInstance si : InformationCenter.getHostList()) {
			if (checkOptimalState(si)) {
				hostStateMap.put(si.getServerConnection().getUrl().toString(),
						"Optimal");
			} else if (checkWarningState(si)) {
				hostStateMap.put(si.getServerConnection().getUrl().toString(),
						"Warning");
			} else if (checkOverloadedState(si)) {
				hostStateMap.put(si.getServerConnection().getUrl().toString(),
						"Overloaded");
			}
		}
		return hostStateMap;
	}

	/**
	 * @return true if all the hosts are in optimal state
	 * @throws IOException
	 */
	public boolean areAllHostOptimal() throws IOException {

		for (ServiceInstance si : InformationCenter.getHostList()) {
			if (!checkOptimalState(si)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @param host
	 *            whose state needs to be checked
	 * @return true if the host is in optimal state else it returns false
	 * @throws IOException
	 */
	public boolean checkOptimalState(ServiceInstance host) throws IOException {

		double mem = getMemoryUsage(host);
		if (mem <= 60) {
			return true;
		}
		return false;
	}

	/**
	 * @param host
	 *            whose state needs to be checked
	 * @return true if the host is in warning state else it returns false
	 * @throws IOException
	 */
	public boolean checkWarningState(ServiceInstance host) throws IOException {
		double mem = getMemoryUsage(host);
		if (mem > 60 && mem <= 85) {
			return true;
		}
		return false;
	}

	/**
	 * @param host
	 *            whose state needs to be checked
	 * @return true if the host is in overloaded state else it returns false
	 * @throws IOException
	 */
	public boolean checkOverloadedState(ServiceInstance host)
			throws IOException {
		double mem = getMemoryUsage(host);
		if (mem > 85 && mem <= 100) {
			return true;
		}
		return false;
	}

	/**
	 * @param host
	 *            whose memory usage needs to be calculated
	 * @return memory usage
	 * @throws IOException
	 */
	public double getMemoryUsage(ServiceInstance host) throws IOException {

		Folder rootFolder = host.getRootFolder();
		ManagedEntity[] meHost = new InventoryNavigator(rootFolder)
				.searchManagedEntities("HostSystem");
		ManagedEntity[] meVMs = new InventoryNavigator(host.getRootFolder())
				.searchManagedEntities("VirtualMachine");
		HostSystem hostSystem = (HostSystem) meHost[0];
		long hostMemory = 0;
		long hostMemUsed = 0;
		long hostMemAvailForVMs = 0;
		long vmsMemory = 0;
		if (hostSystem != null) {
			hostMemory = getHostMemTotal(hostSystem);
			hostMemUsed = getHostMemUsed(hostSystem);
			hostMemAvailForVMs = hostMemory - hostMemUsed;
		}
		for (ManagedEntity managedEntity : meVMs) {
			VirtualMachine vm = (VirtualMachine) managedEntity;
			if (vm != null) {
				if (vm.getRuntime().getPowerState()
						.equals(VirtualMachinePowerState.poweredOn)) {
					vmsMemory += vm.getSummary().getConfig().getMemorySizeMB();
				}
			}
		}

		double memoryUsage = 0.0;
		if (hostMemAvailForVMs != 0) {
			memoryUsage = hostMemAvailForVMs - vmsMemory;
			memoryUsage = memoryUsage / hostMemAvailForVMs;
			memoryUsage = memoryUsage * 100;
			memoryUsage = 100 - memoryUsage;
		}

		return memoryUsage;
	}

	/**
	 * @param host
	 *            whose CPU usage needs to be calculated
	 * @return CPU Usage in percentage
	 * @throws IOException
	 */
	public double getCPUUsage(ServiceInstance host) throws IOException {

		Folder rootFolder = host.getRootFolder();
		ManagedEntity[] meHost = new InventoryNavigator(rootFolder)
				.searchManagedEntities("HostSystem");
		ManagedEntity[] meVMs = new InventoryNavigator(host.getRootFolder())
				.searchManagedEntities("VirtualMachine");
		HostSystem hostSystem = (HostSystem) meHost[0];
		long hostCPUtotal = getHostCpuTotal(hostSystem);
		long hostCPUUsed = getHostCpuUsed(hostSystem);
		long hostCpuAvailForVMs = hostCPUtotal - hostCPUUsed;
		long vmsCpu = 0;

		for (ManagedEntity managedEntity : meVMs) {
			VirtualMachine vm = (VirtualMachine) managedEntity;
			if (vm != null) {
				vmsCpu += getVmCpuUsed(vm);
			}
		}

		double cpuUsage = 0;
		if (hostCpuAvailForVMs != 0) {
			cpuUsage = hostCpuAvailForVMs - vmsCpu;
			cpuUsage = cpuUsage / hostCpuAvailForVMs;
			cpuUsage = cpuUsage * 100;
			cpuUsage = 100 - cpuUsage;
		}

		return cpuUsage;
	}

	/**
	 * This method uses built in methods to get the CPU usage of a host
	 * 
	 * @param host
	 * @return CPU usage
	 * @throws IOException
	 */
	private Integer getHostCpuUsed(HostSystem host) throws IOException {
		Integer usedMhz;
		HostListSummary hostSummary = host.getSummary();
		HostListSummaryQuickStats hostQuickStats = hostSummary.getQuickStats();
		// host.getResourcePool();
		usedMhz = hostQuickStats.getOverallCpuUsage();
		if (usedMhz == null) {
			usedMhz = 0;
		}
		return usedMhz;
	}

	/**
	 * This method uses built in methods to get the total CPU usage of a
	 * host(including the cpu used by VMs)
	 * @param host
	 * @return total CPU usage
	 * @throws IOException
	 */
	private Integer getHostCpuTotal(HostSystem host) throws IOException {
		HostListSummary hls = host.getSummary();
		HostHardwareSummary hosthwi = hls.getHardware();
		Integer totalMhz = hosthwi.getCpuMhz();
		return totalMhz;
	}

	/**
	 * @param host whose memory needs to be calculated
	 * @return memory used by host
	 * @throws IOException
	 */
	private Integer getHostMemUsed(HostSystem host) throws IOException {
		Integer usedMem;
		HostListSummary hostSummary = host.getSummary();
		HostListSummaryQuickStats hostQuickStats = hostSummary.getQuickStats();

		// host.getResourcePool();
		usedMem = hostQuickStats.getOverallMemoryUsage();

		if (usedMem == null) {
			usedMem = 0;
		}
		return usedMem / (1024 * 1024);
	}

	/**
	 * @param host 
	 * @return total memory of the host
	 * @throws IOException
	 */
	private long getHostMemTotal(HostSystem host) throws IOException {
		HostListSummary hls = host.getSummary();
		HostHardwareSummary hosthwi = hls.getHardware();
		long totalMem = hosthwi.getMemorySize() / (1024 * 1024);
		return totalMem;
	}

	/**
	 * @param vm whose CPU usage needs to be calculated
	 * @return CPU used by the vm
	 * @throws IOException
	 */
	private Integer getVmCpuUsed(VirtualMachine vm) throws IOException {
		Integer usedMhz;
		VirtualMachineSummary vmSummary = vm.getSummary();
		VirtualMachineQuickStats vmQuickStats = vmSummary.getQuickStats();

		usedMhz = vmQuickStats.getOverallCpuUsage();
		if (usedMhz == null) {
			usedMhz = 0;
		}
		return usedMhz;
	}

	/**
	 * @return the less utilized host in a cluster
	 */
	public ServiceInstance getLessUtilizedHost() {

		ServiceInstance instance = null;
		for (ServiceInstance si : InformationCenter.getHostList()) {
			/*try {
				if (checkOptimalState(si)) {
					return si;
				}
			} catch (IOException e1) {
				System.out.println(e1.getMessage());
			}*/
			double leaseMemUsed = 100;

			try {
				if (leaseMemUsed > getMemoryUsage(si)) {
					instance = si;
					leaseMemUsed = getMemoryUsage(si);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return instance;
	}

	/**
	 * @param si is the host from where the vm needs to be migrated
	 * @param siNew is the host where vm needs to be migrated
	 * @return the vm with maximum memory usage
	 */
	public String getVMtoBeMigrated(ServiceInstance si, ServiceInstance siNew) {
		String vmTobeMigrated = null;
		List<VirtualMachine> vms = InformationCenter.getVMsOfHost(si);
		if (vms != null && vms.size() > 0) {
			int maxMem = 0;
			for (VirtualMachine virtualMachine : vms) {
				if (virtualMachine.getRuntime().getPowerState()
						.equals(VirtualMachinePowerState.poweredOn)) {
					if (virtualMachine.getSummary().getConfig()
							.getMemorySizeMB() > maxMem) {
						maxMem = virtualMachine.getSummary().getConfig()
								.getMemorySizeMB();
						vmTobeMigrated = virtualMachine.getName();
					}
				}
			}
		}

		return vmTobeMigrated;
	}
}
