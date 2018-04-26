package tr.util.db;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 11/23/2016
 * Time: 11:08 PM
 */
public class SQLiteConnector {
    private static final String URL_PREFIX = "jdbc:sqlite:";

    private final String dbFile;
    private final ThreadLocal<Connection> parallelConnThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Connection> readOnlyConnThreadLocal = new ThreadLocal<>();

    public SQLiteConnector(String dbFile) {
        this.dbFile = dbFile;
    }

    public SQLiteConnector() {
        this(":memory:");
    }


    private Connection openConnection(SQLiteConfig config) throws SQLException {
        return DriverManager.getConnection(URL_PREFIX + dbFile, config.toProperties());
    }

    public Connection openReadOnlyConnection() throws SQLException {
        Connection conn = readOnlyConnThreadLocal.get();

        if (conn == null || conn.isClosed()) {
            final SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(true);
            readOnlyConnThreadLocal.set(conn = openConnection(config));
        }

        return conn;
    }

    public Connection openWritableConnection() throws SQLException {
        final SQLiteConfig config = new SQLiteConfig();
        config.setSynchronous(SQLiteConfig.SynchronousMode.OFF);
        config.enforceForeignKeys(true);
        return openConnection(config);
    }

    public Connection openParallelWritableConnection() throws SQLException {
        Connection conn = parallelConnThreadLocal.get();

        if (conn == null || conn.isClosed()) {
            final SQLiteConfig config = new SQLiteConfig();
            config.setSynchronous(SQLiteConfig.SynchronousMode.OFF);
            config.setOpenMode(SQLiteOpenMode.NOMUTEX);
            parallelConnThreadLocal.set(conn = openConnection(config));
            conn.setAutoCommit(false);
        }

        return conn;
    }
}
