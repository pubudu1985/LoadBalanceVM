/**
 * 
 */
package core;

import com.vmware.vim25.mo.ServiceInstance;

/**
 * @author Kavya
 * 
 */
public interface LoadBalancingStratergy {
	/**
	 * @param hostSystem
	 *            on which the balancing strategy will be applied to make it
	 *            optimal
	 */
	public void runLoadBalancing(ServiceInstance hostSystem);
}
