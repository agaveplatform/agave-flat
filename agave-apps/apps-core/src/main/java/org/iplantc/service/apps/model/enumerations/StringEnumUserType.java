package org.iplantc.service.apps.model.enumerations;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.util.ReflectHelper;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A generic UserType that handles String-based JDK 5.0 Enums.
 * 
 * @author Gavin King
 */
@SuppressWarnings("unchecked")
public class StringEnumUserType implements EnhancedUserType, ParameterizedType {

	@SuppressWarnings("rawtypes")
	private Class<Enum>	enumClass;

	public void setParameterValues(Properties parameters)
	{
		String enumClassName = parameters.getProperty("enumClassname");
		try
		{
			// log.debug("Finding class for name " + enumClassName);
			enumClass = ReflectHelper.classForName(enumClassName);
		}
		catch (ClassNotFoundException cnfe)
		{
			throw new HibernateException("Enum class not found", cnfe);
		}
	}

	public Class<?> returnedClass()
	{
		return enumClass;
	}

	@SuppressWarnings("deprecation")
	public int[] sqlTypes()
	{
		return new int[] { Hibernate.STRING.sqlType() };
	}

	public boolean isMutable()
	{
		return false;
	}

	public Object deepCopy(Object value)
	{
		return value;
	}

	@SuppressWarnings("rawtypes")
	public Serializable disassemble(Object value)
	{
		return (Enum) value;
	}

	public Object replace(Object original, Object target, Object owner)
	{
		return original;
	}

	public Object assemble(Serializable cached, Object owner)
	{
		return cached;
	}

	public boolean equals(Object x, Object y)
	{
		return x == y;
	}

	public int hashCode(Object x)
	{
		return x.hashCode();
	}

	public Object fromXMLString(String xmlValue)
	{
		return Enum.valueOf(enumClass, xmlValue);
	}

	@SuppressWarnings("rawtypes")
	public String objectToSQLString(Object value)
	{
		return '\'' + ( (Enum) value ).name() + '\'';
	}

	@SuppressWarnings("rawtypes")
	public String toXMLString(Object value)
	{
		return ( (Enum) value ).name();
	}

	public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
			throws SQLException
	{
		String name = rs.getString(names[0]);
		return rs.wasNull() ? null : Enum.valueOf(enumClass, name);
	}

	@SuppressWarnings({ "deprecation", "rawtypes" })
	public void nullSafeSet(PreparedStatement st, Object value, int index)
			throws SQLException
	{
		if (value == null)
		{
			st.setNull(index, Hibernate.STRING.sqlType());
		}
		else
		{
			st.setString(index, ( (Enum) value ).name());
		}
	}

}
