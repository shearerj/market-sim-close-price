package entity.agent;

import java.util.Collection;
import java.util.Collections;

import model.MarketModel;
import utils.RandPlus;
import activity.Activity;
import entity.agent.BackgroundAgent;
import entity.market.Market;
import event.TimeStamp;

public class MockAgent extends BackgroundAgent {

	private static final long serialVersionUID = 1L;

	public MockAgent(int agentID, MarketModel model, Market market) {
		super(agentID, new TimeStamp(0), model, market, new PrivateValue(),
				new RandPlus(), 1000);
	}

	@Override
	public Collection<? extends Activity> agentStrategy(TimeStamp currentTime) {
		return Collections.emptySet();
	}

}
