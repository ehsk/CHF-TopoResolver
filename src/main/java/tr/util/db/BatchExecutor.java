package tr.util.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 11/11/2016
 * Time: 2:26 PM
 */
public class BatchExecutor {
    private final String sql;
    private final int maxBatchSize;
    private final ConnectionSupplier connectionSupplier;

    private PreparedStatement pstmt = null;
    private int batchSize = 0;

    public BatchExecutor(ConnectionSupplier connectionSupplier, int maxBatchSize, String sql) {
        this.connectionSupplier = connectionSupplier;
        this.maxBatchSize = maxBatchSize;
        this.sql = sql;
    }

    public void addBatch(Consumer<PreparedStatement> parameterConsumer) throws SQLException {
        if (pstmt == null) {
            initPreparedStatement(connectionSupplier);
        }

        parameterConsumer.accept(pstmt);

        pstmt.addBatch();

        if (++batchSize % maxBatchSize == 0) {
            executeBatch();
            initPreparedStatement(connectionSupplier);
        }
    }

    private void executeBatch() throws SQLException {
        if (pstmt != null) {
            pstmt.executeBatch();
            pstmt.getConnection().commit();
            pstmt.close();
            pstmt.getConnection().close();
        }
    }

    private void initPreparedStatement(ConnectionSupplier connectionSupplier) throws SQLException {
        final Connection conn = connectionSupplier.get();
        pstmt = conn.prepareStatement(sql);
    }

    public void finalizeBatch() throws SQLException {
        executeBatch();
    }

    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }
}
