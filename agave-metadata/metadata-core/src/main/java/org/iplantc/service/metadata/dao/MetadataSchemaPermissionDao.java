/**
 * 
 */
package org.iplantc.service.metadata.dao;

import org.apache.commons.lang.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.service.common.Settings;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.persistence.TenancyHelper;
import org.iplantc.service.metadata.exceptions.MetadataException;
import org.iplantc.service.metadata.model.MetadataSchemaItem;
import org.iplantc.service.metadata.model.MetadataSchemaPermission;
import org.iplantc.service.metadata.model.enumerations.PermissionType;
import org.iplantc.service.metadata.util.ServiceUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DAO class for metadata schemata. This needs moved into the mongodb with the schema records.
 * 
 * @author dooley
 */
public class MetadataSchemaPermissionDao {

	protected static Session getSession() {
		HibernateUtil.beginTransaction();
		Session session = HibernateUtil.getSession();
		//session.clear();
		session.enableFilter("schemaPemTenantFilter").setParameter("tenantId", TenancyHelper.getCurrentTenantId());
		return session;
	}
	
	/**
	 * Returns all metadata schema permissions for the given schemaId.
	 *
	 * @param schemaId the schema id for which we are fetching permissions
	 * @return a list of permissions for the schema id
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	@SuppressWarnings("unchecked")
	public static List<MetadataSchemaPermission> getBySchemaId(String schemaId)
	throws MetadataException
	{

		if (!ServiceUtils.isValid(schemaId))
			throw new MetadataException("Schema id cannot be null");

		try
		{
			Session session = getSession();

			String hql = "from MetadataSchemaPermission where schemaId = :schemaId order by username asc";
			List<MetadataSchemaPermission> pems = session.createQuery(hql)
					.setString("schemaId", schemaId)
					.setCacheMode(CacheMode.IGNORE)
					.setCacheable(false)
					.list();

			session.flush();

			return pems;
		}
		catch (HibernateException ex)
		{
			throw new MetadataException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception ignored) {}
		}
	}
	
	/**
     * Returns the {@link MetadataSchemaPermission#getSchemaId()} of {@link MetadataSchemaItem} to which
     * the user has read permission. Delegates to {@link #getUuidOfAllSharedMetataSchemaItemReadableByUser(String, int, int)}
     * 
     * @param username the user for whom to look up permission grants
     * @return list of uuid to which the user has been granted read access.
     * @throws MetadataException
     */
    public static List<String> getUuidOfAllSharedMetataSchemaItemReadableByUser(String username)
    throws MetadataException
    {
        return getUuidOfAllSharedMetataSchemaItemReadableByUser(username, 0, -1);
    }
	
	/**
	 * Returns the {@link MetadataSchemaPermission#getSchemaId()} of {@link MetadataSchemaItem} to which
     * the user has read permission.
     * 
	 * @param username the user for whom to look up permission grants
	 * @param offset the number of results to skip before returning the response set
	 * @param limit the maximum results to return
	 * @return list of uuid to which the user has been granted read access.
	 * @throws MetadataException
	 */
	@SuppressWarnings("unchecked")
    public static List<String> getUuidOfAllSharedMetataSchemaItemReadableByUser(String username, int offset, int limit)
    throws MetadataException
    {

	    if (StringUtils.isEmpty(username)) {
            throw new MetadataException("Username cannot be null in permission lookup");
        }
	    
        try
        {
            Session session = getSession();
            
            String sql = "SELECT distinct schema_id, last_updated "
					+ "FROM metadata_schema_permissions "
            		+ "WHERE tenant_id = :tenantid "
            		+ "		AND (permission = :allpem OR permission LIKE '%READ%') "
            		+ "		AND username in (:owner, :world, :public) "
            		+ "ORDER BY last_updated DESC";
            Query query = session.createSQLQuery(sql)
                .setString("tenantid", TenancyHelper.getCurrentTenantId())
                .setString("owner", username)
                .setString("world", Settings.WORLD_USER_USERNAME)
                .setString("public", Settings.PUBLIC_USER_USERNAME)
            	.setString("allpem", PermissionType.ALL.name());
            
            if (offset > 0) {
            	query.setFirstResult(offset);
            }
            
            if (limit >= 0) {
            	query.setMaxResults(limit);
            }
            
            List<Object[]> metadataSchemaUUIDGrantedToUser = query
            		.setCacheMode(CacheMode.IGNORE)
					.setCacheable(false)
					.list();
            
            session.flush();

            return metadataSchemaUUIDGrantedToUser.stream().map(row -> (String)row[0]).collect(Collectors.toList());
        }
        catch (HibernateException ex)
        {
            throw new MetadataException(ex);
        }
        finally {
            try { HibernateUtil.commitTransaction(); } catch (Exception ignored) {}
        }
    }

	/**
	 * Gets the metadata permissions for the specified username and iod
	 *
	 * @param username the users to whom the permission is assigned
	 * @param schemaId the id to delete
	 * @return a matching permission if present, false otherwise
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	@SuppressWarnings("unchecked")
	public static MetadataSchemaPermission getByUsernameAndSchemaId(String username, String schemaId) throws MetadataException
	{
		if (!ServiceUtils.isValid(username))
			throw new MetadataException("Username cannot be null");

		try
		{
			Session session = getSession();

			String hql = "from MetadataSchemaPermission where schemaId = :schemaId and username = :username";
			List<MetadataSchemaPermission> pems = session.createQuery(hql)
					.setString("username", username)
					.setString("schemaId", schemaId)
					.setCacheMode(CacheMode.IGNORE)
					.setCacheable(false)
					.list();

			session.flush();

			return pems.isEmpty() ? null : pems.get(0);
		}
		catch (HibernateException ex)
		{
			throw new MetadataException(ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception ignored) {}
		}
	}

	/**
	 * Saves a new metadata permission. Upates existing ones.
	 * @param pem the permission to delete
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	public static void persist(MetadataSchemaPermission pem) throws MetadataException
	{
		if (pem == null)
			throw new MetadataException("Permission cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.saveOrUpdate(pem);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception ignored) {}

			throw new MetadataException("Failed to save metadata Schema permission.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception ignored) {}
		}
	}

	/**
	 * Deletes the given iod permission.
	 *
	 * @param pem the permission to delete
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	public static void delete(MetadataSchemaPermission pem) throws MetadataException
	{
		if (pem == null)
			throw new MetadataException("Permission cannot be null");

		try
		{
			HibernateUtil.beginTransaction();
			Session session = HibernateUtil.getSession();
			session.delete(pem);
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception ignored) {}

			throw new MetadataException("Failed to delete metadata schema permission.", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception ignored) {}
		}
	}

	/**
	 * Deletes all permissions for the metadata schema with given schemaId
	 *
	 * @param schemaId the id to delete
	 * @throws org.iplantc.service.metadata.exceptions.MetadataException
	 */
	public static void deleteBySchemaId(String schemaId) throws MetadataException
	{
		if (schemaId == null) {
			return;
		}

		try
		{
			Session session = getSession();

			String sql = "delete from MetadataSchemaPermission where schemaId = :schemaId";
			session.createQuery(sql)
					.setString("schemaId", schemaId)
					.executeUpdate();
			
			session.flush();
		}
		catch (HibernateException ex)
		{
			try
			{
				if (HibernateUtil.getSession().isOpen()) {
					HibernateUtil.rollbackTransaction();
				}
			}
			catch (Exception ignored) {}
			
			throw new MetadataException("Failed to delete metadata schema permission", ex);
		}
		finally {
			try { HibernateUtil.commitTransaction(); } catch (Exception ignored) {}
		}
	}

}
