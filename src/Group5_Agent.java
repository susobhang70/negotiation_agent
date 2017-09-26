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
public class Group5_Agent extends AbstractNegotiationParty {

	private double fDiscountFactor = 0; // if you want to keep the discount
										// factor
	private double fReservationValue = 0; // if you want to keep the reservation
											// value
	private double fBeta = 1.2;
	
	private List<BidDetails> lOutcomeSpace;
	
	private int nRounds, nCurrentRound, nCount;

	@Override
	public void init(AbstractUtilitySpace utilSpace, Deadline dl,
			TimeLineInfo tl, long randomSeed, AgentID agentId)
	{

		super.init(utilSpace, dl, tl, randomSeed, agentId);

		fDiscountFactor = utilSpace.getDiscountFactor(); // read discount factor
		System.out.println("Discount Factor is " + fDiscountFactor);
		fReservationValue = utilSpace.getReservationValueUndiscounted(); // read
																		// reservation
																		// value
		System.out.println("Reservation Value is " + fReservationValue);

		// if you need to initialize some variables, please initialize them
		// below

		lOutcomeSpace = new SortedOutcomeSpace(this.utilitySpace).getAllOutcomes();
		
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

		// with 50% chance, counter offer
		// if we are the first party, also offer.
		nCurrentRound++;
		Bid newbid = lOutcomeSpace.get(nCount).getBid();
		nCount++;
		if(getUtility(newbid) < fReservationValue)
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
	}

	@Override
	public String getDescription() {
		return "example party group N";
	}

}
