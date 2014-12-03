package entity;

import event.TimeStamp;

/**
 * A view is an object that represents possibly delayed access to some object
 * (usually an entity) in the simulation. It allows latent access to be easy to
 * enforce, and allows for stricter access controls on objects. For example, an
 * agent has access to market views, not actual markets. This way, when an agent
 * submits an order, it will be delayed if necessary without any effort on the
 * agent implementer.
 * 
 * By default, it should be possible to know how latent an actual view is.
 * 
 * @author erik
 * 
 */
public interface View {

	public TimeStamp getLatency();
	
}
