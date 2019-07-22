import genius.core.Bid;
import genius.core.issue.Issue;
import java.util.HashMap;
import java.util.Map;

public class Agent {

    private static int idCounter = 0;

    private int agent_Id;

    private HashMap<String,Double> issues;
    private HashMap<String,String> valueToIssue;
    private HashMap<String,Integer> values;


    public Agent(){

        issues = new HashMap<>();
        valueToIssue = new HashMap<>();
        values = new HashMap<>();

        idCounter++;
        agent_Id = idCounter;
    }

    public void addIssueValue(String issue,Double value){
        issues.put(issue,value);
    }

    public  void  addIssueToValue(String issue,String value){
        valueToIssue.put(value,issue);
    }
    public void addValues(String value,Integer number){
        values.put(value,number);
    }

    public void setValueToIssue(HashMap<String, String> valueToIssue) {
        this.valueToIssue = valueToIssue;
    }

    public void setValues(HashMap<String, Integer> values) {
        this.values = values;
    }

    public double calculateUtility(Bid offer){
        double utility = 0;

        for (Issue issue:offer.getIssues()) {
            String issueName = issue.toString();


            for (Map.Entry<String, String> entry : valueToIssue.entrySet())
            {
                // Check if value matches with given value
                if (entry.getValue().equals(issueName) )
                {
                    if (entry.getKey().equals(offer.getValue(issue).toString()))
                    utility += issues.get(issueName) * values.get(entry.getKey());
                }
            }
        }

      //   System.out.println(offer + "UTILITY : " + utility);

        return utility;
    }


    public void printAgent(){
        for (Map.Entry<String,Double> issue : issues.entrySet()) {
            String issueName = issue.getKey();
            System.out.println("ISSUE NAME: " + issueName  +  " = " + issue.getValue());

            for (Map.Entry<String, String> entry : valueToIssue.entrySet())
            {
                // Check if value matches with given value
                if (entry.getValue().equals(issueName))
                {
                    System.out.println("VALUE NAME: " + entry.getKey().toString() + " = " + values.get(entry.getKey().toString()));
                }
            }
        }
        
        
    }
}
