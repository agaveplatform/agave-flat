package org.iplantc.service.common.persistence.time;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Handles conversion of {@link java.util.Date} to CDT (America/Chicago) when 
 * reading to/from the db.
 * 
 * @author dooley
 *
 */
public class InstantTypeDescriptor extends TimestampTypeDescriptor {
    
	private static final long serialVersionUID = -828569458015830360L;

	public static final InstantTypeDescriptor INSTANCE = new InstantTypeDescriptor();

//    private static final TimeZone CDT = TimeZone.getTimeZone("America/Chicago");

    public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicBinder<X>( javaTypeDescriptor, this ) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                st.setTimestamp( index, javaTypeDescriptor.unwrap( value, Timestamp.class, options ));
            }
        };
    }

    public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicExtractor<X>( javaTypeDescriptor, this ) {
            @Override
            protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
                return javaTypeDescriptor.wrap( rs.getTimestamp( name ), options );
            }
        };
    }
}