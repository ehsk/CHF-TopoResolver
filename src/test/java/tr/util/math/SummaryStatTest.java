package tr.util.math;

import tr.util.math.statistics.SampledSummaryStat;
import tr.util.math.statistics.SummaryStat;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 7/27/2017
 * Time: 3:06 AM
 */
class SummaryStatTest {

    @Test
    void testMeasures() {
        final ArrayList<Integer> data = Lists.newArrayList(1, 2, 3, 2, 1);
        SummaryStat stat = new SummaryStat();
        data.forEach(stat::accept);

        final IntSummaryStatistics actualStat = data.stream().mapToInt(Integer::intValue).summaryStatistics();

        assertEquals(actualStat.getCount(), stat.getCount());
        assertEquals(actualStat.getSum(), stat.getSum());
        assertEquals(actualStat.getAverage(), stat.getAverage(), 0.001);
        assertEquals(actualStat.getMin(), stat.getMin());
        assertEquals(actualStat.getMax(), stat.getMax());
        assertEquals(0.837, stat.getStdev(), 0.001);
        assertEquals(0.7, stat.getVariance(), 0.001);
    }

    @Test
    void testEmptyMeasures() {
        SummaryStat stat = new SummaryStat();

        assertEquals(0, stat.getCount());
        assertEquals(0, stat.getSum());
        assertEquals(0, stat.getAverage());
        assertEquals(0, stat.getStdev());
        assertEquals(0, stat.getVariance());
    }

    @Test
    void testMedianOnOddSize() {
        final ArrayList<Integer> data = Lists.newArrayList(1, 2, 3, 2, 1);
        SampledSummaryStat stat = new SampledSummaryStat();
        data.forEach(stat::accept);

        assertEquals(2, stat.getMedian());
    }

    @Test
    void testMedianOnEvenSize() {
        final ArrayList<Integer> data = Lists.newArrayList(1, 2, 3, 3, 1, 5);
        SampledSummaryStat stat = new SampledSummaryStat();
        data.forEach(stat::accept);

        assertEquals(2.5, stat.getMedian(), 0.001);
    }
}
