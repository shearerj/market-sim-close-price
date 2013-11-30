package entity.agent;

public class ZIPAgentTest {

	
//	// Asserting that margin updated correctly
//	if (isBuyer) {
//		if (lastTransPrice.lessThanEqual(lastPrice)) 
//			assert Math.abs(oldMargin) <= Math.abs(margin); // raise margin
//		else
//			assert Math.abs(oldMargin) >= Math.abs(margin); // lower margin
//	} else {
//		if (lastTransPrice.greaterThanEqual(lastPrice))
//			assert Math.abs(oldMargin) <= Math.abs(margin); // raise margin
//		else
//			assert Math.abs(oldMargin) >= Math.abs(margin); // lower margin
//	}

}


//package entity;
//
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import static org.junit.Assert.assertTrue;
//
//import data.*;
//import event.*;
//import market.*;
//import model.*;
//import systemmanager.*;
//
//
//public class ZIPAgentTest {
//
//	private ZIPAgent a;
//	private static MarketModel model;
//	private static Market market;
//	private static SIP sip;
//	private static SystemData data;
//	private static Log l;
//	private static int mktID;
//	private static int modelID;
//	private int agentID;
//	private int seed;		// pseudorandom number generator seed
//	private int buyerIDIndex;
//	private int sellerIDIndex;
//	private int buyOrderIndex;
//	private int sellOrderIndex;
//	private double tempA1;
//	private double tempA2;
//	private double tempA3;
//	private double tempA4;
//	private double tempA5;
//	private double tempA6;
//	private double tempB1;
//	private double tempB2;
//	private double tempB3;
//	private double tempB4;
//	private double tempB5;
//	private double tempB6;
//	private double RC1;
//	private double RC2;
//	private double RC3;
//	private double RC4;
//	private double RC5;
//	private double RC6;
//	private double AC1;
//	private double AC2;
//	private double AC3;
//	private double AC4;
//	private double AC5;
//	private double AC6;
//	
//	
//	@BeforeClass
//	public static void setupClass() {
//		data = new SystemData();
//		try {
//			l = new Log(0, ".", "test.txt", true);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		mktID = -1;
//		market = new CDAMarket(mktID, data, Consts.getProperties(Consts.CDA), l);
//		ObjectProperties p = new ObjectProperties();
//		p.put(Consts.MODEL_CONFIG_KEY, "1");
//		modelID = 1;
//		model = new CentralCDA(modelID, p, data);
//		
//		market.linkModel(modelID);
//		model.getMarketIDs().add(mktID);
//		data.addMarket(market);
//		data.addModel(model);
//		
//		sip = new SIP(0, data, l);
//		data.setSIP(sip);
//	}
//	
//	@Before
//	public void setup() {
//		// setting buyer / seller IDs, plus order IDs, for the transactions
//		buyerIDIndex = 100;
//		sellerIDIndex = 200;
//		buyOrderIndex = 0;
//		sellOrderIndex = 1000;
//		
//		agentID = 1;
//		seed = 12345;
//		ObjectProperties p = new ObjectProperties(Consts.getProperties(Consts.ZIP));
//		p.put(SMAgent.MARKETID_KEY, Integer.toString(mktID));
//		p.put(Agent.RANDSEED_KEY, Integer.toString(seed));
//		p.put(Agent.ARRIVAL_KEY, Integer.toString(100));	// arrives at time 100
//		// note that the ZIP must arrive AFTER its transactions happen
//		
//		a = new ZIPAgent(agentID, 1, data, p, l);
//		model.linkAgent(agentID);
//		
//		// transaction of 1 unit at 100
//		Transaction t = new PQTransaction(1, new Price(100), buyerIDIndex++, 
//				sellerIDIndex++, buyOrderIndex++, sellOrderIndex++, new TimeStamp(0), mktID);
//		a.transactions.add(t);
//		
//		Transaction b = new PQTransaction(1, new Price(0), buyerIDIndex++, 
//				sellerIDIndex++, buyOrderIndex++, sellOrderIndex++, new TimeStamp(0), mktID);
//		a.transactions.add(b);
//				
//		Transaction c = new PQTransaction(1, new Price(100000), buyerIDIndex++, 
//				sellerIDIndex++, buyOrderIndex++, sellOrderIndex++, new TimeStamp(0), mktID);
//		a.transactions.add(c);
//	}
//
//	@Test
//	public void TestMargin() {
//		// FILL IN
//		((ZIPAgent)a).beta = 1.0;
//		
//		
//		assertTrue("My test", ((ZIPAgent)a).computeDelta(2) == -1.4338603187071666);
//	}
//
//	@Test
//	public void TestMomentum() {
//		
//		
//	}
//	
//	@Test
//	public void TestCoeffR(){
//
//		RC1 = ((ZIPAgent)a).computeRCoefficient(true);
//		
//		assertTrue("CoeffR", RC1 > 1 && RC1 < 1 + ((ZIPAgent)a).rangeCoeffR);
//		RC2 = ((ZIPAgent)a).computeRCoefficient(false);
//		assertTrue("CoeffR", RC2 < 1 && RC2 > 1 - ((ZIPAgent)a).rangeCoeffR);
//		RC3 = ((ZIPAgent)a).computeRCoefficient(true);
//		assertTrue("CoeffR", RC3 > 1 && RC3 < 1 + ((ZIPAgent)a).rangeCoeffR);
//		RC4 = ((ZIPAgent)a).computeRCoefficient(true);
//		assertTrue("CoeffR", RC4 > 1 && RC4 < 1 + ((ZIPAgent)a).rangeCoeffR);
//		RC5 = ((ZIPAgent)a).computeRCoefficient(true);
//		assertTrue("CoeffR", RC5 > 1 && RC5 < 1 + ((ZIPAgent)a).rangeCoeffR);
//		RC6 = ((ZIPAgent)a).computeRCoefficient(true);
//		assertTrue("CoeffR", RC6 > 1 && RC6 < 1 + ((ZIPAgent)a).rangeCoeffR);
//		
//		
//		double temp;
//		temp = ((ZIPAgent)a).computeRCoefficient(false);
//		assertTrue("CoeffR", temp < 1 && temp > 1 - ((ZIPAgent)a).rangeCoeffR);
//		temp = ((ZIPAgent)a).computeRCoefficient(false);
//		assertTrue("CoeffR", temp < 1 && temp > 1 - ((ZIPAgent)a).rangeCoeffR);
//
//	}
//	
//	@Test
//	public void TestCoeffA(){
//		AC1 = ((ZIPAgent)a).computeACoefficient(true);
//		
//		assertTrue("CoeffR", AC1 < 0 && AC1 > ((ZIPAgent)a).rangeCoeffA);
//		AC2 = ((ZIPAgent)a).computeACoefficient(false);
//		assertTrue("CoeffR", AC2 > 0 && AC2 < -((ZIPAgent)a).rangeCoeffA);
//		AC3 = ((ZIPAgent)a).computeACoefficient(true);
//		assertTrue("CoeffR", AC3 < 0 && AC3 > ((ZIPAgent)a).rangeCoeffA);
//		AC4 = ((ZIPAgent)a).computeACoefficient(true);
//		assertTrue("CoeffR", AC4 < 0 && AC4 > ((ZIPAgent)a).rangeCoeffA);
//		AC5 = ((ZIPAgent)a).computeACoefficient(true);
//		assertTrue("CoeffR", AC5 < 0 && AC5 > ((ZIPAgent)a).rangeCoeffA);
//		AC6 = ((ZIPAgent)a).computeACoefficient(true);
//		assertTrue("CoeffR", AC6 < 0 && AC6 > ((ZIPAgent)a).rangeCoeffA);
//		
//		double temp;
//		temp = ((ZIPAgent)a).computeACoefficient(true);
//		assertTrue("CoeffR", temp < 0 && temp > ((ZIPAgent)a).rangeCoeffA);
//		
//		temp = ((ZIPAgent)a).computeACoefficient(false);
//		assertTrue("CoeffR", temp > 0 && temp < -((ZIPAgent)a).rangeCoeffA);
//		temp = ((ZIPAgent)a).computeACoefficient(false);
//		assertTrue("CoeffR", temp > 0 && temp < -((ZIPAgent)a).rangeCoeffA);
//		temp = ((ZIPAgent)a).computeACoefficient(false);
//		assertTrue("CoeffR", temp > 0 && temp < -((ZIPAgent)a).rangeCoeffA);
//		temp = ((ZIPAgent)a).computeACoefficient(false);
//		assertTrue("CoeffR", temp > 0 && temp < -((ZIPAgent)a).rangeCoeffA);
//	}
//	
//	@Test
//	public void TestComputeTargetPrice(){
//	//	System.out.println("___________________________________");
//		tempA1 = ((ZIPAgent)a).computeTargetPrice(5);
//	  	assertTrue("comp1", tempA1 == 1.715741448991189);
//	  	
//	  	tempA2 = ((ZIPAgent)a).computeTargetPrice(0);
//	  	assertTrue("comp2", tempA2 == 8.615331890951182);
//	  	
//	  	tempA3 = ((ZIPAgent)a).computeTargetPrice(1000);
//	  	assertTrue("comp3", tempA3 == 953.2617254021436);
//	  	
//	  	tempA4 = ((ZIPAgent)a).computeTargetPrice(12);
//	  	assertTrue("comp4", tempA4 == -1.2192228260956774);
//	  	
//	  	tempA5 = ((ZIPAgent)a).computeTargetPrice(100000);
//	  	assertTrue("comp5", tempA5 == 123092.33741851835);
//	  	
//	  	tempA6 = ((ZIPAgent)a).computeTargetPrice(1);
//	  	assertTrue("comp6", tempA6 == -26.970436142684513);
//		
//	}
//	
//	@Test
//	public void testComputeDelta(){
//		
//		assertTrue("...", ((ZIPAgent)a).computeDelta(5) == 1.715741448991189);
//		assertTrue("...", ((ZIPAgent)a).computeDelta(0) == 8.615331890951182);
//		assertTrue("...", ((ZIPAgent)a).computeDelta(1000) == 953.2617254021436);
//		assertTrue("...", ((ZIPAgent)a).computeDelta(12) == -1.2192228260956774);
//		assertTrue("...", ((ZIPAgent)a).computeDelta(100000) == 123092.33741851835);
//		assertTrue("...", ((ZIPAgent)a).computeDelta(1) == -26.970436142684513);
//		
//	}
//
//	@Test
//	public void testUpdateMomentumChange(){
//		
////		change = gamma * change + (1-gamma) * delta;
//		((ZIPAgent)a).momentumChange = 2;
//		((ZIPAgent)a).gamma = 3;
//		
//		((ZIPAgent)a).updateMomentumChange(5);
//		assertTrue("...", ((ZIPAgent)a).momentumChange == 2.568517102017622);
//		
//		((ZIPAgent)a).updateMomentumChange(0);
//		assertTrue("...", ((ZIPAgent)a).momentumChange == -9.5251124758495);
//
//		((ZIPAgent)a).updateMomentumChange(1000);
//		assertTrue("...", ((ZIPAgent)a).momentumChange == -1935.0987882318357);
//		
//		((ZIPAgent)a).updateMomentumChange(12);
//		assertTrue("...", ((ZIPAgent)a).momentumChange == -5802.857919043316);
//		
//		((ZIPAgent)a).updateMomentumChange(100000);
//		assertTrue("...", ((ZIPAgent)a).momentumChange == -263593.24859416665);
//		
//		((ZIPAgent)a).updateMomentumChange(0);
//		assertTrue("...", ((ZIPAgent)a).momentumChange == -790923.2138087429);
//
//	}
//	
//	@Test
//	public void testComputeMargin(){
//		
//		((ZIPAgent)a).limitPrice = 5;
//		
//		((ZIPAgent)a).computeMargin(5);
//		assertTrue("...", 	((ZIPAgent)a).margin == -0.8627406840807048);		
//		
//		((ZIPAgent)a).computeMargin(0);
//		assertTrue("...", 	((ZIPAgent)a).margin == -0.22841785917232826);
//		
//		((ZIPAgent)a).computeMargin(1000);
//		assertTrue("...", 	((ZIPAgent)a).margin == 75.72388731666811);
//		
//		((ZIPAgent)a).computeMargin(12);
//		assertTrue("...", 	((ZIPAgent)a).margin == 44.93679456391321);
//		
//		((ZIPAgent)a).computeMargin(100000);
//		assertTrue("...", 	((ZIPAgent)a).margin == 9873.949070219816);
//		assertTrue(".", ((ZIPAgent)a).delta == 123092.33741851835);
//		
//		((ZIPAgent)a).computeMargin(1);
//		assertTrue("...", 	((ZIPAgent)a).margin == 5921.811807240475);
//		assertTrue(".", ((ZIPAgent)a).delta == -26.970436142684513);
//
//	}
//	
//	@Test
//	public void testCollection(){
//		long lon = 1;
//		TimeStamp t = new TimeStamp(lon);
//		((ZIPAgent)a).agentStrategy(t);
//	}
//	
////	@Test
////	public void ExtraTest() {
////		for(int i=0; i < 100; i++) {
////			setup();
////			Test();
////		}
////	}
//}
