import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.Domain;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.ValueDiscrete;
import negotiator.issue.Objective;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.session.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;
import negotiator.utility.AdditiveUtilitySpace;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;

/**
 * This is your negotiation party.
 */
@SuppressWarnings("unused")
public class Group5 extends AbstractNegotiationParty
{

	private static final double defectDiscount = 0.9;
	private static final double epsilon = 0.02;
	private static final double minIncrement = 0.000001;

	private double fDiscountFactor = 0;
	private double fReservationValue = 0;
	private double fBeta = 1.2;

	private int nRounds, nCurrentRound, nCount, nIssues;
	private int nRejectOffers, nValues;
	private Bid lastBid;
	private Domain domain;
	
	private List<BidDetails> lOutcomeSpace;
	private List<Issue> lIssue;
	private HashMap<AgentID, List<Bid>> bidHistory;
	private HashMap<AgentID, Bid> highestOfferedBids;
	private HashMap<Issue, List<ValueDiscrete>> hValuesOfIssue;
	private HashMap<Issue, Integer> lValuesPerIssue;
	private HashMap<AgentID, AdditiveUtilitySpace> lAgentUtilSpaces;


	@Override
	public void init(AbstractUtilitySpace utilSpace, Deadline dl,
			TimeLineInfo tl, long randomSeed, AgentID agentId)
	{
		super.init(utilSpace, dl, tl, randomSeed, agentId);

		fDiscountFactor = utilSpace.getDiscountFactor(); // read discount factor
		System.out.println("Discount Factor is " + fDiscountFactor);
		fReservationValue = utilSpace.getReservationValueUndiscounted();

		System.out.println("Reservation Value is " + fReservationValue);

		lAgentUtilSpaces = new HashMap<AgentID, AdditiveUtilitySpace>();

		lOutcomeSpace = new SortedOutcomeSpace(this.utilitySpace).getAllOutcomes();
		domain = this.utilitySpace.getDomain();
		lIssue = domain.getIssues();

		bidHistory = new HashMap<AgentID, List<Bid>>();
		highestOfferedBids = new HashMap<AgentID, Bid>();
		lValuesPerIssue = new HashMap<Issue, Integer>();
		hValuesOfIssue = new HashMap<Issue, List<ValueDiscrete>>();

		nRounds = this.deadlines.getValue();
		nIssues = lIssue.size();
		nCurrentRound = 0;
		nCount = 0;
		
		for (Map.Entry<Objective, Evaluator> e : ((AdditiveUtilitySpace)this.utilitySpace).getEvaluators())
		{
			int tempValues = 0;
			Iterator<?> it = ((IssueDiscrete)e.getKey()).getValues().iterator();
			
			List <ValueDiscrete> tempList = new ArrayList<ValueDiscrete>();
			while (it.hasNext())
			{
				ValueDiscrete value = (ValueDiscrete)it.next();
				tempList.add(value);
				nValues++;
				tempValues++;
			}
			lValuesPerIssue.put((Issue)e.getKey(), tempValues);
			hValuesOfIssue.put((Issue)e.getKey(), tempList);
		}

		System.out.println("Values: " + nValues);
		System.out.println(lValuesPerIssue.toString());

		// number of rounds to defect in order to read the preferences of other agents
		if(fDiscountFactor != 1.0D)
			nRejectOffers = (int) (Math.log(defectDiscount) / Math.log(fDiscountFactor)) + 1;
		else
			nRejectOffers = 50;
	}

	/**
	 * Each round this method gets called and ask you to accept or offer. The
	 * first party in the first round is a bit different, it can only propose an
	 * offer.
	 *
	 * @param validActions
	 *            Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions)
	{
		nCurrentRound++;
		
		// TODO add final round decision
		
		if(getUtility(this.lastBid) >= Group5.defectDiscount)
			return new Accept();

		if(nCurrentRound <= nRejectOffers)
		{
			SortedOutcomeSpace sortSpace = new SortedOutcomeSpace(this.utilitySpace);
			Bid newbid = sortSpace.getBidNearUtility(Group5.defectDiscount).getBid();
			this.lastBid = newbid;
			return new Offer(newbid);
		}
		
		// this is whether to accept the last bid
		int satisfies = 0;
		double lastUtil = this.utilitySpace.getUtility(this.lastBid);
		for(Map.Entry<AgentID, AdditiveUtilitySpace> e: this.lAgentUtilSpaces.entrySet())
		{
			double otherUtil = e.getValue().getUtility(this.lastBid);
			if(lastUtil > (otherUtil + 0.2) && lastUtil > this.fReservationValue)
				satisfies++;
		}
		
		if(satisfies == this.lAgentUtilSpaces.size())
			return new Accept();
		
		// The following is a fail safe
		Bid newbid = lOutcomeSpace.get(nCount).getBid();
		Bid oppHighBid = null;

		nCount++;
		if(getUtility(newbid) < Math.min(0.5 + fReservationValue * 1.3, getUtility(lOutcomeSpace.get(0).getBid())))
		{
			nCount = 0;
			newbid = lOutcomeSpace.get(nCount).getBid();
		}
		this.lastBid = newbid;

		// This is the optimized function
		TreeMap<Integer, TreeMap<Double, BidDetails>> bidEvals = this.optimize();
		for(Map.Entry<Integer, TreeMap<Double, BidDetails>> e : bidEvals.entrySet())
		{
			TreeMap<Double, BidDetails> tempMap = e.getValue();
			if(tempMap == null)
				continue;
			double minUtil = 2.0;
			double maxOppUtil = 0.0;
			for(BidDetails bid: tempMap.values())
			{
				double tempUtil = 0.0;
				for(Map.Entry<AgentID, AdditiveUtilitySpace> e1: this.lAgentUtilSpaces.entrySet())
					tempUtil += (e1.getValue().getUtility(bid.getBid()) + 0.13);
				if(this.utilitySpace.getUtility(bid.getBid()) < minUtil)
				{
					newbid = bid.getBid();
					minUtil = this.utilitySpace.getUtility(bid.getBid());
				}
				if(tempUtil > maxOppUtil)
				{
					oppHighBid = bid.getBid();
					maxOppUtil = tempUtil;
				}
			}
//			this.lastBid = newbid;
//			System.out.print("["+e.getKey()+"]" + newbid.toString() + " Mine: " + minUtil);
//			for(Map.Entry<AgentID, AdditiveUtilitySpace> e1: this.lAgentUtilSpaces.entrySet())
//				System.out.print(e1.getKey().toString() + " " + e1.getValue().getUtility(newbid) + " ");
//			System.out.println();

			this.lastBid = oppHighBid;
			System.out.print("["+e.getKey()+"]" + oppHighBid.toString() + " Mine: " + this.utilitySpace.getUtility(oppHighBid) +" ");
			for(Map.Entry<AgentID, AdditiveUtilitySpace> e1: this.lAgentUtilSpaces.entrySet())
				System.out.print(e1.getKey().toString() + " " + e1.getValue().getUtility(oppHighBid) + " ");
			System.out.println();
			break;
		}

		return new Offer(newbid);
	}
	
	private Double getSkewedUtility(Bid bid)
	{
		Double util = ((getUtility(bid)/fDiscountFactor) - fReservationValue)/(1 - fReservationValue);
		Double timeUtil = 1 - java.lang.Math.pow(this.timeline.getCurrentTime()/this.timeline.getTotalTime(), fBeta);
		util = util * timeUtil;
		return util;
	}

	/**
	 * All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict
	 * their utility.
	 *
	 * @param sender
	 *            The party that did the action. Can be null.
	 * @param action
	 *            The action that party did.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action) 
	{
		super.receiveMessage(sender, action);

		if(action instanceof Offer || sender != null)
		{
			if(action instanceof Offer)
			{
				this.lastBid = ((Offer)action).getBid();
				if(!this.highestOfferedBids.containsKey(sender))
					this.highestOfferedBids.put(sender, this.lastBid);
				
				if(getUtility(this.lastBid) > getUtility(this.highestOfferedBids.get(sender)))
					this.highestOfferedBids.put(sender, this.lastBid);
			}

			List<Bid> bidList;
			if(!this.bidHistory.containsKey(sender))
			{
				this.bidHistory.put(sender, bidList = new ArrayList<Bid>());
				initializeUtilitySpace(sender);
			}

			bidList = this.bidHistory.get(sender);
			bidList.add(lastBid);
			
			this.updateAgentModel(sender);
		}
	}

	@Override
	public String getDescription()
	{
		return "Group 5 Improved Agent";
	}

	private void initializeUtilitySpace(AgentID agent)
	{
		AdditiveUtilitySpace utilSpace = ((AdditiveUtilitySpace)this.utilitySpace.copy());

		double w = 1.0 / nIssues;

		// this technically sets all the issue weights equally at the beginning for a particular agent's utility space
		for (Map.Entry<Objective, Evaluator> e : utilSpace.getEvaluators())
		{
			utilSpace.unlock((Objective)e.getKey());
			((Evaluator)e.getValue()).setWeight(w);

			// here all the values (discrete domain) of the particular issue is given equal weights, of 1 initially
			Iterator<?> it = ((IssueDiscrete)e.getKey()).getValues().iterator();
			while (it.hasNext())
			{
				ValueDiscrete value = (ValueDiscrete)it.next();
				try 
				{
					((EvaluatorDiscrete)e.getValue()).setEvaluation(value, 1);
				}
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
			}
		}

		this.lAgentUtilSpaces.put(agent, utilSpace);
	}

	private HashMap<Integer, Integer> bidChanges(AgentID agent)
	{
		if(this.bidHistory.get(agent).size() < 2)
			return null;

		HashMap<Integer, Integer> changes = new HashMap<Integer, Integer>();
		List<Bid> lAgentBids = this.bidHistory.get(agent);

		Bid agentLastBid = lAgentBids.get(lAgentBids.size() - 1);
		Bid agentSecondLastBid = lAgentBids.get(lAgentBids.size() - 2);

		for(Issue i : lIssue)
		{
			if( ((ValueDiscrete)agentLastBid.getValue(i.getNumber())).equals(
				(ValueDiscrete)agentSecondLastBid.getValue(i.getNumber()) ) )
				changes.put(i.getNumber(), 0);

			else
				changes.put(i.getNumber(), 1);
		}

		return changes;
	}

	private void updateAgentModel(AgentID agent)
	{
		if(this.bidHistory.get(agent).size() < 2)
			return;

		AdditiveUtilitySpace agentUtilSpace = lAgentUtilSpaces.get(agent);
		HashMap<Integer, Integer> changes = bidChanges(agent);
		int nChanged = 0;

		// so we calculate the number of issues changed by the agent from its last bid
		for(Issue i : lIssue)
			if(changes.get(i.getNumber()) == 0)
				nChanged++;

		double fIncrement = 0.04;
		double fCurrentSum = 1.0 + (fIncrement * nChanged);
		double fMaxWeight = 1.0 - (nIssues * fIncrement / fCurrentSum);

		// here we update the weights of each of the changed issues, and normalize the rest so that sum is 1
		for(Issue i : lIssue)
		{
			double fUpdatedWt;
			double fCurWt = agentUtilSpace.getWeight(i.getNumber());

			if(changes.get(i.getNumber()) == 0 &&  fCurWt < fMaxWeight)
				fUpdatedWt = (fCurWt + fIncrement) / fCurrentSum;
			else
				fUpdatedWt = fCurWt / fCurrentSum;

			agentUtilSpace.setWeight(domain.getObjective(i.getNumber()), fUpdatedWt);
		}

		List<Bid> agentBidHistory = this.bidHistory.get(agent);
		Bid agentLastBid = agentBidHistory.get(agentBidHistory.size() - 1);
		Iterator<?> it = agentUtilSpace.getEvaluators().iterator();

		while(it.hasNext())
		{
			Map.Entry<Objective, Evaluator> e = (Map.Entry)it.next();

			ValueDiscrete value = (ValueDiscrete) agentLastBid.getValue(((IssueDiscrete)e.getKey()).getNumber());
			int nOldValue;
			try 
			{
				nOldValue = ((EvaluatorDiscrete)e.getValue()).getEvaluationNotNormalized(value);
				((EvaluatorDiscrete)e.getValue()).setEvaluation(value, 1 + nOldValue);
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
			}
		}

		lAgentUtilSpaces.put(agent, agentUtilSpace);
	}
	
	private TreeMap<Integer, TreeMap<Double, BidDetails>> optimize()
	{
		if(this.nCurrentRound < this.nRejectOffers)
			return null;

		TreeMap<Integer, TreeMap<Double, BidDetails>> allBidEvals = 
				new TreeMap<Integer, TreeMap<Double, BidDetails>>(Collections.reverseOrder());
		
		System.out.println(this.lOutcomeSpace.size());
		
		for(BidDetails bid: this.lOutcomeSpace)
		{
			double tempUtil = this.utilitySpace.getUtility(bid.getBid());
			int satisfies = 0;
			for(Map.Entry<AgentID, AdditiveUtilitySpace> e: this.lAgentUtilSpaces.entrySet())
			{
				double otherUtil = e.getValue().getUtility(bid.getBid());
				if(tempUtil > (otherUtil + 0.2) && tempUtil > this.fReservationValue)
				{
					satisfies++;
//					System.out.println(bid.getBid().toString() + " Mine: " + tempUtil + " Other: " + otherUtil);
				}
			}
			if(satisfies > 0)
			{
				if(!allBidEvals.containsKey(satisfies))
					allBidEvals.put(satisfies, new TreeMap<Double, BidDetails>(Collections.reverseOrder()));
				TreeMap<Double, BidDetails> tempMap = allBidEvals.get(satisfies);
				while(tempMap.containsKey(tempUtil))
					tempUtil += minIncrement;
				tempMap.put(tempUtil, bid);
				allBidEvals.put(satisfies, tempMap);
			}
		}
		return allBidEvals;
	}
	
	private double getValueEvaulation(AbstractUtilitySpace utilSpace, Issue issue, ValueDiscrete value) throws Exception
	{
		return ((EvaluatorDiscrete)((AdditiveUtilitySpace)utilSpace).getEvaluator(issue.getNumber())).getEvaluation(value);
	}
}
