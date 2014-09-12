package core;

import java.rmi.RemoteException;

import com.vmware.vim25.DuplicateName;
import com.vmware.vim25.FileFault;
import com.vmware.vim25.InsufficientResourcesFault;
import com.vmware.vim25.InvalidDatastore;
import com.vmware.vim25.InvalidName;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.OutOfBounds;
import com.vmware.vim25.ResourcePoolResourceUsage;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineConfigSummary;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineQuestionInfo;
import com.vmware.vim25.VmConfigFault;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.ResourcePool;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class VM {
	private String name;
	private String memory;
	private int cpu;
	private String os;

	public VM(String name, String mem, int cpu, String os) {
		this.name = name;
		this.memory = mem;
		this.cpu = cpu;
		this.os = os;
		//addVM(this);
	}

	public VM() {
	}

	public boolean addVM(VM vm) {
		
		if (InformationCenter.getVM(vm.name) != null) {
			return false;
		}
		else {
		/*
		 * Create a VM configuration specification. For our implementation, this
		 * specification contains name, memory, CPU, OS and data storage file path
		 * for a VM. Any further information like, disk etc can be added as well.
		 */
		
		VirtualMachineConfigSpec config = CreateVMSpec(vm);

		/*
		 * Create this VM with a random host. While creating a VM, it can be
		 * registered with any host. It does not consume any resources until it
		 * is powered on. May be we can ask user to enter a host as well. If
		 * host is given, use that else add to the first host in list
		 */
		
		//Get default host service instance.
		ServiceInstance si = InformationCenter.getHost();
		
		//Get folder to create VM task in 
		Folder vmf = InformationCenter.getVMFolder(si);

		//Get resource pool to be used
		ResourcePool pool = InformationCenter.getResourcePool(si);

		//Get host system to be used.
		HostSystem host = InformationCenter.getHostSystem(si);

		//Create VM task
		try {
			Task vmTask = vmf.createVM_Task(config, pool, host);
			//Wait for Create VM task to complete
			InformationCenter.waitForTaskCompletion(vmTask);
			
			//Once the task is completed, add new created VM to the Information Center list.
			InformationCenter.updateVMList(si);			
		} catch (InvalidName e) {
			e.printStackTrace();
		} catch (VmConfigFault e) {
			e.printStackTrace();
		} catch (DuplicateName e) {
			e.printStackTrace();
		} catch (FileFault e) {
			e.printStackTrace();
		} catch (OutOfBounds e) {
			e.printStackTrace();
		} catch (InsufficientResourcesFault e) {
			e.printStackTrace();
		} catch (InvalidDatastore e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}	
		System.out.println();
		System.out.println("Virtualmachine " + vm.getName() + "Created successfully!!!");
		System.out.println();
		return true;
		}
	}

	public void powerON(String name) {
		/*
		 *  List handling will be done at back end of GUI.
		 *  Sort the selected VMs in decreasing order of memory requirement
		 *  For each selected VM call vm.powerON(name) by passing name of the VM as parameter.		
		 */
		//Retrieve VM from VMList matching the passed name.
		VirtualMachine vm = InformationCenter.getVM(name);
		
		// Get parent folder of the VM
		//Folder origVMFolder = (Folder) vm.getParent();
		
		
		//Get its configuration summary and alloted memory
		VirtualMachineConfigSummary vmsummary = vm.getSummary().getConfig();
		Integer memory = vmsummary.getMemorySizeMB();
		
		//Get host service instance by running the algorithm for initial placement 
		ServiceInstance si = initialPlacement(memory.longValue());
		
		// Get VMFolder, data store path, resource pool and host system to register the VM and then Power it on.
		//Folder vmFolder = InformationCenter.getVMFolder(si);
		HostSystem host = InformationCenter.getHostSystem(si);
		
		//if (origVMFolder.equals(vmFolder)) {
		
		if (vm.getServerConnection().getUrl().equals(si.getServerConnection().getUrl())) {
			try {
				Task tskVMOn = vm.powerOnVM_Task(host);
				InformationCenter.waitForTaskCompletion(tskVMOn);
			} catch (VmConfigFault e) {
				e.printStackTrace();
			} catch (TaskInProgress e) {
				e.printStackTrace();
			} catch (FileFault e) {
				e.printStackTrace();
			} catch (InvalidState e) {
				e.printStackTrace();
			} catch (InsufficientResourcesFault e) {
				e.printStackTrace();
			} catch (RuntimeFault e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			System.out.println();
			System.out.println("Virtualmachine " + vm.getName() + "PoweredOn successfully!!!");
			System.out.println();
		}
		else {
			registerPowerON(si, name);
		}
	}	//End of method PowerON

	 public void registerPowerON(ServiceInstance si, String name) {
		//Retrieve VM from VMList matching the passed name.
		VirtualMachine vm = InformationCenter.getVM(name);
		 Folder vmFolder = InformationCenter.getVMFolder(si);
		 String dsPath = getDSPath(name);
			ResourcePool rsp = InformationCenter.getResourcePool(si);
			HostSystem host = InformationCenter.getHostSystem(si);
			try {
				//VM needs to be registered to a new host before power on.
				Task regTask = vmFolder.registerVM_Task(dsPath, name, false, rsp, host);
				
				//Delete the VM registered with previous host from the list.
				InformationCenter.deleteVMfromList(name);
				
				// Unregister VM from the previous host
				vm.unregisterVM();
				
				//Wait for completion of register VM task before updating VM list
				InformationCenter.waitForTaskCompletion(regTask);
				
				//Update the VM list to have VM registered with the new host and retrieve the VM into VM instance.
				InformationCenter.updateVMList(si);
				vm = InformationCenter.getVM(name);
				
				//Once registered and retrieved, Power on the machine.
				Task tskVMOn = vm.powerOnVM_Task(host);
				
				//Sleep for 100 ms for task to get initiated.
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					System.out.println("Thread interrupted.");
					e1.printStackTrace();
				}
				
				/*
				 * When a machine is powered on first time after registering to a new host, a VM question is asked - 
				 * to confirm if the machine is moved or copied.
				 * This question is asked when task is 95% complete and needs to be answered to PowerOn the machine.
				 * Thus we need to wait until the PowerOn task starts running and is 95% complete.
				 */
				while (tskVMOn.getTaskInfo().getState() != TaskInfoState.running || tskVMOn.getTaskInfo().getProgress()<95) {
					try {
						Thread.sleep(100);
						if (tskVMOn.getTaskInfo().getState() != TaskInfoState.success) {
							break;
						}
					} catch (InterruptedException e) {
						System.out.println("Thread interrupted.");
						e.printStackTrace();
					}
				}
				
				/*
				 * After re-registering a VM, we need to answer the question if VM is moved or copied. 
				 * For our purpose, it is moved.
				 * Choice option for move is 1. It can be changed to 2 for Copy.
				 */
				//Sleep for 500 ms for task to get initiated.
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
				System.out.println("This VM is currently running tasks on other hosts, Hence cannot be powered on.");
				e.printStackTrace();
			} catch (FileFault e) {
				System.out.println("VM file invalid");
				e.printStackTrace();
			} catch (InvalidState e) {
				System.out.println("VM not in a valid state to Power on.");
				e.printStackTrace();
			} catch (InsufficientResourcesFault e) {
				System.out.println("Host doesnot have sufficient resources to power on the VM");
				e.printStackTrace();
			} catch (RuntimeFault e) {
				e.printStackTrace();
			} catch (RemoteException e) {
				e.printStackTrace();
			}	
			System.out.println();
			System.out.println("Virtualmachine " + vm.getName() + "Registered and PoweredOn successfully!!!");
			System.out.println();
	}	//End of method registerPowerOn
	
	public ServiceInstance initialPlacement(Long mem) {
		// Get the host with max memory available
		// Return this host
		ServiceInstance siRet = InformationCenter.getHost();
		ResourcePool defRsp = InformationCenter.getResourcePool(siRet);
		Long maxMem = (defRsp.getRuntime().getMemory().getMaxUsage()) - (defRsp.getRuntime().getMemory().getReservationUsed());

		//Iterate through the list of available hosts
		for (ServiceInstance si : InformationCenter.getHostList()){
			//For each service instance get the resource pool and its configuration status.
			ResourcePool rsp = InformationCenter.getResourcePool(si);
			String rspStatus = rsp.getConfigStatus().toString();
			
			//Check if resource pool status is green - Under utilized: Still have enough resources to run another VM.
			if (rspStatus.equalsIgnoreCase("Green")){

				//Retrieve runtime resource usage properties for a given resource pool.
				ResourcePoolResourceUsage rspUsage = rsp.getRuntime().getMemory();
				
				//Max memory usage allowed
				Long maxUsage = rspUsage.getMaxUsage();
				
				//Memory already used
				Long reservUsed = rspUsage.getReservationUsed();
				
				//Available Memory = Max Usage - Already used
				Long availMem = maxUsage - reservUsed;
				
				//Check if required memory can be accommodated in this resource pool.
				if (mem<availMem) {
				
					//Check if current resource has max available memory
					if (availMem>maxMem) {
						siRet = si;
						maxMem = availMem;
					}	// End of inner most if
				}	// End of if
			}	//End of outer most if
		}	//End of for loop
		return siRet;
	}	// End of method initialPlacement

	public void powerOFF(String name) {
		/*
		 * Similar to powerON, list handling needs to be done at GUI back end
		 * For each VM in list call powerOFF by passing VM name as parameter
		 */
		VirtualMachine vm = InformationCenter.getVM(name);
		try {
			Task powerOffTask = vm.powerOffVM_Task();
			
			//Wait for PowerOff task to complete successfully
			InformationCenter.waitForTaskCompletion(powerOffTask);
			System.out.println();
			System.out.println("Virtualmachine " + vm.getName() + "PoweredOff successfully!!!");
			System.out.println();
		} catch (TaskInProgress e) {
			System.out.println("This VM is currently running tasks, Hence cannot be powered off.");
			e.printStackTrace();
		} catch (InvalidState e) {
			System.out.println("VM not in a valid state to Power off.");
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public VirtualMachineConfigSpec CreateVMSpec(VM vm) {
		//Specifying configuration to create a new virtual machine.
		VirtualMachineConfigSpec vmSpec = new VirtualMachineConfigSpec();
		vmSpec.setName(vm.getName());
		vmSpec.setMemoryMB(new Long(Integer.parseInt(vm.getMem())));
		vmSpec.setNumCPUs(vm.getCPU());

		//Guest Id for the operating system that needs to be installed.
		vmSpec.setGuestId(vm.getOS());

		//Set file path where .vmx file for a vm has to be stored.
		vmSpec.files = new VirtualMachineFileInfo();

		//Data storage name needs to be updated later once we have our system setup done.
		String path = getDSPath(name);
		vmSpec.files.vmPathName = path;

		return vmSpec;
	}

	
	/*
	 * Getter and setter methods for each private data member
	 */
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getMem() {
		return memory;
	}
	public void setMemory(String mem) {
		this.memory = mem;
	}
	public int getCPU() {
		return cpu;
	}
	public void setCPU(int cpu) {
		this.cpu = cpu;
	}
	public String getOS() {
		return os;
	}
	public void setOS(String os) {
		this.os = os;
	}
	public String getDSPath(String name) {
		return "[nas]" + name + "/" + name + ".vmx";
	}
}

