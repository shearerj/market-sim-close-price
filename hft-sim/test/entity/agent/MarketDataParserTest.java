package entity.agent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Iterator;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import entity.agent.MarketDataParser.MarketAction;
import entity.agent.MarketDataParser.SubmitOrder;
import entity.agent.MarketDataParser.WithdrawOrder;

public class MarketDataParserTest {

	@Test
	public void nyseSimpleParseTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNYSE(new StringReader("A,0,1,P,B,5742346,SRG1,3249052,0,88,O,AARCA,\n"));
		assertTrue("Incorrect number of actions", actions.hasNext());
		assertTrue("Incorrect action type", actions.next() instanceof SubmitOrder);
		assertFalse("Incorrect number of actions", actions.hasNext());
	}
	
	@Test
	public void nyseDeleteParseTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNYSE(new StringReader(
				"A,0,1,B,B,981477,SRG2,5516081,0,0,L,AARCA,\n" +
				"D,0,1,2,0,SRG2,B,L,AARCA,B,\n"));
		assertTrue("Incorrect number of actions", actions.hasNext());
		assertTrue("Incorrect action type", actions.next() instanceof SubmitOrder);
		assertTrue("Incorrect number of actions", actions.hasNext());
		assertTrue("Incorrect action type", actions.next() instanceof WithdrawOrder);
		assertFalse("Incorrect number of actions", actions.hasNext());
	}
	
	@Test
	public void nyseTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNYSE(new StringReader(
				"A,1,1,B,B,981477,SRG2,5516081,0,0,L,AARCA,\n" +
				"M,2,2,7603174,3068063,0,0,SRG3,B,E,AARCA,B,\n" +
				"D,3,3,0,0,SRG5,N,L,AARCA,S,\n" +
				"A,4,4,P,S,1747834,SRG3,1177542,0,0,B,AARCA,\n" +
				"D,5,5,0,0,SRG1,P,L,AARCA,S,\n" +
				"D,6,6,0,0,SRG4,N,E,AARCA,S,\n" +
				"D,0,7,0,75,SRG1,B,L,AARCA,B,\n" +
				"M,1,8,2803365,3525506,0,75,SRG4,N,L,AARCA,B,\n" +
				"M,0,9,549587,3436753,0,147,SRG3,B,O,AARCA,S,\n" +
				"A,0,10,B,S,3921581,SRG4,5223572,0,192,O,AARCA,\n" +
				"M,0,11,8516210,62759,0,278,SRG2,N,O,AARCA,B,\n" +
				"D,0,12,0,326,SRG4,N,L,AARCA,S,\n" +
				"M,0,9,549587,3436753,0,147,SRG3,B,O,AARCA,S,\n"));
		
		Iterator<Class<? extends MarketAction>> types = ImmutableList.of(
				SubmitOrder.class,
				WithdrawOrder.class,
				SubmitOrder.class,
				WithdrawOrder.class,
				WithdrawOrder.class,
				WithdrawOrder.class,
				SubmitOrder.class,
				WithdrawOrder.class).iterator();
		
		while (actions.hasNext() && types.hasNext())
			assertTrue("Incorrect action type", types.next().isAssignableFrom(actions.next().getClass()));
		assertFalse("Too many actions", actions.hasNext());
		assertFalse("Too few actions", types.hasNext());
	}
	
	@Test
	public void nasdaqSimpleParseTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNasdaq(new StringReader(
				"T,0\n" +
				"A,16382751,1,B,3748742,SRG1,5630815\n"));
		assertTrue("Incorrect number of actions", actions.hasNext());
		assertTrue("Incorrect action type", actions.next() instanceof SubmitOrder);
		assertFalse("Incorrect number of actions", actions.hasNext());
	}

	@Test
	public void nasdaqDeleteParseTest() {
		Iterator<MarketAction> actions = MarketDataParser.parseNasdaq(new StringReader(
				"T,0\n" +
				"A,16382751,1,B,3748742,SRG1,5630815\n" +
				"D,20000000,1\n"));
		assertTrue("Incorrect number of actions", actions.hasNext());
		assertTrue("Incorrect action type", actions.next() instanceof SubmitOrder);
		assertTrue("Incorrect number of actions", actions.hasNext());
		assertTrue("Incorrect action type", actions.next() instanceof WithdrawOrder);
		assertFalse("Incorrect number of actions", actions.hasNext());
	}
	
}
