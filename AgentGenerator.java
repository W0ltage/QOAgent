import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class AgentGenerator {

    private boolean debug_mode = false;

    private List<Agent> agents;
    private List<Issue> issues;
    private HashMap<String,String> valueToIssue;
    private HashMap<String,Integer> values;


    private double interestCoefficient;
    private double averageUtil;


    private void createValues(){
        for (int i = 0; i < issues.size(); i++){
            String issueName = issues.get(i).toString();
            IssueDiscrete issueDiscrete = (IssueDiscrete) issues.get(i);
            for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
                Random rnd = new Random();
                String valueName = valueDiscrete.getValue();
                valueToIssue.put(valueName,issueName);
                values.put(valueName,rnd.nextInt(20) + 10);
            }
        }
    }

    public AgentGenerator(int numberOfAgents , List<Issue> issues){

        if (numberOfAgents == 0){
            numberOfAgents = issues.size();
        }else if (numberOfAgents > issues.size()) throw new IllegalArgumentException("NUMBER OF AGENT CANT BE MORE THEN ISSUES");

        valueToIssue = new HashMap<>();
        values = new HashMap<>();
        agents = new ArrayList<>();
        this.issues = issues;

        interestCoefficient = (double)1 / numberOfAgents;
        averageUtil = (double) 1 / issues.size();

        createValues();
        createAgents(numberOfAgents);

        if (debug_mode) {
            System.out.println("NUMBER OF AGENT: " + numberOfAgents);
            System.out.println("NUMBER OF ISSUE: " + issues.size());
            System.out.println("NUMBER OF ChiLDREN"+issues.get(0).getChildren().size());
        }

    }

    public void createAgents(int number){

        for (int i = 1 ; i <= number; i++){

            Agent agent = new Agent();
            agent.setValueToIssue(valueToIssue);
            agent.setValues(values);

            if (debug_mode) {
                System.out.println("interestCOff: " + interestCoefficient);
                System.out.println("averageUtil: " + averageUtil);
            }
            int focusedIssueNumber = (int)(issues.size() * interestCoefficient * i);

            for (int j = 0; j < focusedIssueNumber; j++){ // generate focused utility
                double utility = averageUtil;
                utility += ((issues.size() - focusedIssueNumber) * (averageUtil / 2)) / focusedIssueNumber;
                agent.addIssueValue(issues.get(j).toString(),utility);
                if (debug_mode) {
                    System.out.println("FOCUSED");
                    System.out.println("ADDED ISSUE NAME: " + issues.get(j).toString() + "VAL: " + utility);
                }
            }
            for (int k = focusedIssueNumber; k < issues.size(); k++){ // generate rest of it
                agent.addIssueValue(issues.get(k).toString(),averageUtil / 2);
                    if (debug_mode) {
                        System.out.println("OTHERS");
                        System.out.println("ADDED ISSUE NAME: " + issues.get(k).toString() + "VAL: " + averageUtil / 2);
                    }
            }
            agents.add(agent);
        }
    }

    public List<Agent> getAgents() {
        return agents;
    }
}
