package eu.trentorise.smartcampus.mobility.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import javax.sql.DataSource;

import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.JdbcTokenStore;

public class FixedSerialVersionUUIDJdbcTokenStore extends JdbcTokenStore {
    public FixedSerialVersionUUIDJdbcTokenStore(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    protected OAuth2Authentication deserializeAuthentication(byte[] authentication) {
        return deserialize(authentication);
    }

    @Override
    protected OAuth2AccessToken deserializeAccessToken(byte[] token) {
        return deserialize(token);
    }

    @Override
    protected OAuth2RefreshToken deserializeRefreshToken(byte[] token) {
        return deserialize(token);
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(byte[] authentication) {
        try {
            return (T) super.deserializeAuthentication(authentication);
        } catch (Exception e) {
            try (ObjectInputStream input = new FixSerialVersionUUID(authentication)) {
                return (T) input.readObject();
            } catch (IOException | ClassNotFoundException e1) {
            	e1.printStackTrace();
                throw new IllegalArgumentException(e1);
            }
        }
    }
}

class FixSerialVersionUUID extends ObjectInputStream {

    public FixSerialVersionUUID(byte[] bytes) throws IOException {
        super(new ByteArrayInputStream(bytes));
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

        ObjectStreamClass mostRecentSerialVersionUUID = ObjectStreamClass.lookup(Class.forName(resultClassDescriptor.getName()));
        return mostRecentSerialVersionUUID;
        
//        if (resultClassDescriptor.getName().equals(SimpleGrantedAuthority.class.getName())) {
//            ObjectStreamClass mostRecentSerialVersionUUID = ObjectStreamClass.lookup(SimpleGrantedAuthority.class);
//            return mostRecentSerialVersionUUID;
//        }
//
//        return resultClassDescriptor;
    }
}        