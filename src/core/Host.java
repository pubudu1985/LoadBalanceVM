/**
 * 
 */
package core;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * this class holds all the host information
 * 
 */
public class Host {

	private String url;
	private String userName;
	private String password;

	public Host(String url, String userName, String password) {
		this.url = url;
		this.userName = userName;
		this.password = password;
	}

	/*
	 * Method for adding the host details to information center's hostList
	 * 
	 * @param host - host that needs to be added
	 */
	public boolean addHost(Host host) {
		if (host != null && host.getUrl() != null) {
			ServiceInstance instance;
			try {
				InformationCenter.hostSystemList.add(host);
				instance = new ServiceInstance(new URL(host.getUrl()),
						host.getUserName(), host.getPassword(),true);
				InformationCenter.getHostList().add(instance);
				InformationCenter.updateVMList(instance);
				HostSystem hostSystem = InformationCenter.getHostSystem(instance);
				InformationCenter.warningStateCount.put(
						hostSystem.getServerConnection().getUrl().toString(), 0);
				
				HostHistory.addNewHistoryRecord1(hostSystem);
				HostHistory.addNewHistoryRecord2(hostSystem);
				return true;
			} catch (RemoteException e) {
				e.printStackTrace();
				return false;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			}

		}
		return false;
	}

	/**
	 * @param si is the host instance 
	 * @return virtual machine which is taking maximum memory from si
	 */
	public VirtualMachine getVmMaxMemUsage(ServiceInstance si) {
		Folder rootFolder = InformationCenter.getRootFoler(si);
		VirtualMachine vmMaxMemUsage = null;
		ManagedEntity[] mes;
		try {
			mes = new InventoryNavigator(rootFolder)
					.searchManagedEntities("VirtualMachine");
			double maxMemUsed = 0;
			if (mes.length > 0) {
				for (ManagedEntity managedEntity : mes) {
					VirtualMachine vm = (VirtualMachine) managedEntity;
					if (vm.getRuntime().getMaxMemoryUsage() > maxMemUsed) {
						maxMemUsed = vm.getRuntime().getMaxMemoryUsage();
						vmMaxMemUsage = vm;
					}
				}
			}
		} catch (InvalidProperty e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (RuntimeFault e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (RemoteException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		return vmMaxMemUsage;

	}

	
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName
	 *            the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
