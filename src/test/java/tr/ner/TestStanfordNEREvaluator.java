package tr.ner;

import tr.dataset.AnnotatedDatasets;
import tr.dataset.DatasetException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 10/12/2016
 * Time: 11:08 PM
 */
class TestStanfordNEREvaluator {

    private final StanfordNEREvaluator evaluator = new StanfordNEREvaluator();

    @Test
    void testEvaluateOnOurDataset() {
        try {
            evaluator.evaluate(AnnotatedDatasets.TRNews);
        } catch (DatasetException e) {
            fail(e.getMessage());
        }
    }

    @Disabled
    @Test
    void testText() {
        evaluator.evaluateText("Controversial pastor picked for Mansfield patrol program. By Vickie Welborn vwelborn@gannett.com Gannett March 24, 2009 MANSFIELD -- Mansfield Mayor Curtis McCoy wants to start a pastor on patrol program for the Mansfield Police Department and he's selected as his first candidate a self-described minister with a controversial past that includes time spent on the other side of the jail bars and troubles with the court system. The Rev. Roy Jones' most recent job was a convenience store clerk for which he had to obtain an alcoholic beverage control card so that he could sell alcoholic beverages. \"This is something I'm trying to do to make a better relationship between the police and the citizens,\" McCoy said of Jones' appointment. Unlike a similar program run by the Shreveport Police Department where ministers volunteer their time to ride on patrol with officers, Mansfield's Pastor on Patrol won't depend upon volunteers - or at this point anyone beyond Jones. Jones will be paid $9 an hour and work 24 hours a week. Asked why he didn't use Shreveport's program as a model, McCoy answered, \"I don't know what Shreveport does nor do I care to know what Shreveport does on that line. I only know what Mansfield is doing. \"McCoy would not comment on Jones' checkered past or his ability to remain unbiased in situations involving the police and citizenry since he is on the city's payroll. \"Based on his relationship in the community as far as knowing people, every time I go to church, I see him in a pulpit - I'm serious. I'm talking the honest God truth. & If he's not pastoring, he's there as a guest. But he knows the people in the community, and I think over the years he has proven himself to be an asset. \"Part-time employees can work six months before being presented to the Board of Aldermen. That's why Jones' official appointment was news to two Mansfield aldermen. Mitch Lewis and G.B. \"Sonny\" Hall were unaware of Jones' addition to the payroll. Lewis admitted that he was questioned over the weekend about Jones being seen last week in a patrol car with a police officer. \"I talked to Roy today (at the council meeting),\" Lewis said Monday night. \"He said yes he was doing it, but he didn't go into it. He said he was pastor on patrol. He said he was trying to get involved with the youth on the weekend and stuff like, which I think is good, but & I don't know. I think (McCoy's) intentions were good to try to set up that type of program. \"Added Hall: \"Most true pastors volunteer their time to do that. \"Jones, who was unseated in October 2007 from his first term on the DeSoto Parish Police Jury, is a close friend of McCoy's. He's aided the mayor in his two election campaigns and frequently accompanies him to functions. Jones did not return a message left on his answering machine Monday. Jones grabbed his own headlines while on the parish governing body. He was consistently involved in employee issues and public criticism of the road department supervisor, including filing complaints with the state attorney general's office that were later deemed unfounded. The Mansfield resident got his own hand slapped in 2005 when the district attorney ordered him to reimburse the parish $175 for gravel he directed onto a private driveway. He has prior arrests for trespassing, issuing worthless checks, failure to appear in court for criminal neglect of family and for failing to report to court on issuing worthless check charges. While he spent time in the DeSoto Detention Center for his arrests, Jones has avoided jail sentences by paying off the insufficient funds and the late child support payments. McCoy said he would later evaluate the program and add a \"white minister, too, because we want everyone to have someone they can talk to.\" Jones is black.");
    }
}
