import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.BidIterator;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;

public class QOAgent extends AbstractNegotiationParty {

    // 0 means max number of agent
    // You cant enter more than issue size
    private static final int DATABASE_AGENT_NUMBER = 0;

    private Bid lastReceivedBid = null;
    private Bid[] allBidsArray;
    private Double[] ourSortedBidArr;
    private Double[] oppSortedBidArr;
    private Double[][] QOValueArray;
    private ArrayList<Bid> allBidsList = new ArrayList<>();
    private List<Agent> sampleAgents;

    private int selectedAgent = 0;
    private int bidCount = 0;
    private double threshHoldValue = 0.05;
    private double reservationValue;

    @Override
    public void init(NegotiationInfo info) {
        super.init(info);
        BidIterator iterator = new BidIterator(utilitySpace.getDomain());
        calculateBidCount(iterator);
        AgentGenerator agentGenerator = new AgentGenerator(DATABASE_AGENT_NUMBER,utilitySpace.getDomain().getIssues());
        sampleAgents = agentGenerator.getAgents();
        reservationValue = utilitySpace.getReservationValue();
        createAndSortArr();
        calculateQOvalues();
    }

    public Bid generateBid() {
        int index = findMaxQOIndex();
        return allBidsArray[index];
    }

    public int findMaxQOIndex() {
        int max = 0;
        double value = 0;
        for (int i = 0; i < QOValueArray.length; i++) {
            if (value <= QOValueArray[i][0]) {
                value = QOValueArray[i][0];
                max = i;
            }
        }
        return max;
    }

    private void calculateQOvalues(){
        for (int i = 0; i < bidCount; i++)
            QOValueArray[i][0] = QOValue(allBidsArray[i]);
    }


    public double QOValue(Bid offer) {
        double alpha = alphaValue(offer);
        double beta = betaValue(offer);
        return Math.min(alpha, beta);
    }

    public double alphaValue(Bid offer) {
        return calculateOfferRank(offer, false) * calculateLuceNumber(offer, false);
    }

    public double calculateOfferRank(Bid offer, boolean isOpp) {
        double util = (isOpp) ? calculateOppUtil(offer,-1) : utilitySpace.getUtility(offer);
        for (int i = 0; i < bidCount; i++) {
            if (isOpp) {
                if (oppSortedBidArr[i] == util)
                    return (i + 1) / bidCount;
            } else {
                if (ourSortedBidArr[i] == util)
                    return (i + 1) / bidCount;
            }
        }
        return -1;
    }

    public double calculateLuceNumber(Bid offer, boolean isOpp) {
        double util = (isOpp) ? calculateOppUtil(offer,-1) : utilitySpace.getUtility(offer);
        double sumOfUtils = 0;
        for (int i = 0; i < bidCount; i++)
            sumOfUtils += (isOpp) ? calculateOppUtil(allBidsArray[i],-1) : utilitySpace.getUtility(allBidsArray[i]);
        return util / sumOfUtils;
    }

    public double betaValue(Bid offer) {
        return (calculateLuceNumber(offer, false) + calculateLuceNumber(offer, true)) * calculateOfferRank(offer, true);
    }

    public void calculateBidCount(BidIterator iterator) {
        while (iterator.hasNext()) {
            Bid bid = iterator.next();
            allBidsList.add(bid);
            bidCount++;
        }
    }

    public void createAndSortArr() {
        allBidsArray = new Bid[bidCount];
        QOValueArray = new Double[bidCount][1];
        ourSortedBidArr = new Double[bidCount];
        oppSortedBidArr = new Double[bidCount];
        for (int i = 0; i < bidCount; i++) {
            allBidsArray[i] = allBidsList.get(i);
            ourSortedBidArr[i] = utilitySpace.getUtility(allBidsList.get(i));
            oppSortedBidArr[i] = calculateOppUtil(allBidsList.get(i),-1);
        }
        quickSort(oppSortedBidArr, 0, bidCount - 1);
        quickSort(ourSortedBidArr, 0, bidCount - 1);
    }


    public double calculateOppUtil(Bid offer , int agentNumber) {
        if (agentNumber == -1)
        return sampleAgents.get(selectedAgent).calculateUtility(offer);

        return sampleAgents.get(agentNumber).calculateUtility(offer);
    }

    public void updateAgentType(){
        HashMap<Integer,Double> luceMap = new HashMap<>();
        HashMap<Integer,Double> resultMap = new HashMap<>();

        double maxProb = 0;

        double prob = 1/sampleAgents.size();
        double denominator = 0;

        for (int i = 0; i < sampleAgents.size() ; i++) {

            double util = calculateOppUtil(lastReceivedBid,i);
            double sumOfUtils = 0;

            for (int j = 0; j < bidCount; j++)
                sumOfUtils += (calculateOppUtil(allBidsArray[i],i));

            double luceNumber = util / sumOfUtils;
            luceMap.put(i,luceNumber);
        }

        for (Map.Entry<Integer,Double> entry: luceMap.entrySet()) {
            denominator += entry.getValue();
        }
        denominator *= prob;
        for (int k = 0; k < sampleAgents.size() ; k++) {
            double result = (luceMap.get(k) * prob) / denominator;
            resultMap.put(k,result);
        }
        for (Map.Entry<Integer,Double> entry: resultMap.entrySet()) {
            if (entry.getValue() > maxProb ) {
                maxProb = entry.getValue();
                selectedAgent = entry.getKey();
            }
        }

    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) {
        if(lastReceivedBid == null)
          return new Offer(getPartyId(), generateBid());

        updateAgentType();
        Bid nextOffer = generateBid();
        double oppUtil = calculateOppUtil(lastReceivedBid,-1);
        double receivedBidUtil = utilitySpace.getUtility(lastReceivedBid);
        double nextRoundBidUtil = utilitySpace.getUtility(nextOffer);
        double nextRoundOppBidUtil = calculateOppUtil(nextOffer,-1);


        if (receivedBidUtil >= nextRoundBidUtil)
            return new Accept(getPartyId(), lastReceivedBid);

        if (Math.abs(nextRoundOppBidUtil - oppUtil) <= threshHoldValue)
            return new Offer(getPartyId(), generateBid());

        double probAcceptance = calculateOfferRank(lastReceivedBid, false);

        double randomdNum = Math.random();

        if (receivedBidUtil >= reservationValue && randomdNum > probAcceptance)
            return new Accept(getPartyId(), lastReceivedBid);

        return new Offer(getPartyId(), generateBid());
    }

    @Override
    public void receiveMessage(AgentID sender, Action action) {
        super.receiveMessage(sender, action);
        if (action instanceof Offer) {
            lastReceivedBid = ((Offer) action).getBid();
        }
    }

    @Override
    public String getDescription() {
        return "QOAgentS";
    }

    private int partition(Double arr[], int low, int high) {
        double pivot = arr[high];
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                Double temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        Double temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return i + 1;
    }

    private void quickSort(Double arr[], int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }
}