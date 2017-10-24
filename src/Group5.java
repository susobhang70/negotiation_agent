import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import negotiator.AgentID;
import negotiator.Bid;
import negotiator.Deadline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.session.TimeLineInfo;
import negotiator.utility.AbstractUtilitySpace;

/**
 * This is your negotiation party.
 */
@SuppressWarnings("unused")
public class Group5 extends AbstractNegotiationParty
{

	private double fDiscountFactor = 0;
	private double fReservationValue = 0;

	private double fBeta = 1.2;
	
	private List<BidDetails> lOutcomeSpace;
	private int nRounds, nCurrentRound, nCount;

	private HashMap<AgentID, List<Bid>> bidHistory;
	private Bid lastBid;

	@Override
	public void init(AbstractUtilitySpace utilSpace, Deadline dl,
			TimeLineInfo tl, long randomSeed, AgentID agentId)
	{

		super.init(utilSpace, dl, tl, randomSeed, agentId);

		fDiscountFactor = utilSpace.getDiscountFactor(); // read discount factor
		System.out.println("Discount Factor is " + fDiscountFactor);
		fReservationValue = utilSpace.getReservationValueUndiscounted();

		System.out.println("Reservation Value is " + fReservationValue);

		lOutcomeSpace = new SortedOutcomeSpace(this.utilitySpace).getAllOutcomes();

		bidHistory = new HashMap<AgentID, List<Bid>>();

		nRounds = this.deadlines.getValue();
		nCurrentRound = 0;
		nCount = 0;
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

		if(nCurrentRound >= nRounds - 5 && getUtility(this.lastBid) > fReservationValue)
			return new Accept();

		Bid newbid = lOutcomeSpace.get(nCount).getBid();

		nCount++;
		if(getUtility(newbid) < Math.min(0.5 + fReservationValue * 1.3, getUtility(lOutcomeSpace.get(0).getBid())))
		{
			nCount = 0;
			newbid = lOutcomeSpace.get(nCount).getBid();
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
		// Here you hear other parties' messages

		if(action instanceof Offer)
		{
			this.lastBid = ((Offer)action).getBid();

			List<Bid> bidList;
			if(!this.bidHistory.containsKey(sender))
				this.bidHistory.put(sender, bidList = new ArrayList<Bid>());

			bidList = this.bidHistory.get(sender);
			bidList.add(lastBid);
		}
	}

	@Override
	public String getDescription()
	{
		return "Group 5 Improved Agent";
	}

}
