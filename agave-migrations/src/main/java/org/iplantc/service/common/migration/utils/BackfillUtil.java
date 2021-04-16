/**
 * 
 */
package org.iplantc.service.common.migration.utils;

import org.iplantc.service.common.uuid.AgaveUUID;
import org.iplantc.service.common.uuid.UUIDType;

import java.sql.*;



/**
 * Helper class to backfill various types of domain object fields.
 * 
 * @author dooley
 *
 */
public class BackfillUtil {
    
	/**
     * Fills the {@code uuid} field of the given table with a valid, unique
     * {@link AgaveUUID} if not already present.
     * 
     * @param connection the current database connectino passed in by Flyway
     * @param tableName the table to udpate
     * @param uuidType the type of UUID to place in the column
     * @throws SQLException when the sql update fails
     */
    public static void backfillAgaveUUID(Connection connection, String tableName, UUIDType uuidType) throws SQLException {
        PreparedStatement updateStmt = connection.prepareStatement("update " + tableName + " set uuid = ? where id = ?");
        Statement stmt = connection.createStatement();
        int fetchSize = 100;
        try {
        	
        	ResultSet rs = stmt.executeQuery("SELECT count(id) FROM " + tableName + " where uuid is null or uuid = ''");
        	rs.next();
        	long totalResults = rs.getLong(1);
            
        	rs = stmt.executeQuery("SELECT id FROM " + tableName + " where uuid is null or uuid = ''");
            stmt.setFetchSize(fetchSize);
            long i=0;
            while (rs.next()) {
            	i++;
            	updateStmt.setString(1,new AgaveUUID(uuidType).toString());
            	updateStmt.setLong(2,rs.getLong(1));
            	updateStmt.addBatch();
                if (i%fetchSize == 0) {
                	updateStmt.executeBatch();
                	System.out.println(String.format("[%d/%d] Migrating %s table...", i, totalResults, tableName));
                }
            }
            updateStmt.executeBatch(); 
        } finally {
            stmt.close();
            updateStmt.close();
        }
    }
}
