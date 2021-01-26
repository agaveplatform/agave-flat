package org.iplantc.service.common.auth;

import org.restlet.Context;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Request;

import java.util.List;

public class NullGuard extends AbstractGuard {

	public NullGuard(Context context, ChallengeScheme scheme, String realm,
			List<Method> unprotectedMethods) throws IllegalArgumentException
	{
		super(context, scheme, realm, unprotectedMethods);
	}
	
	public boolean checkSecret(Request request, String identifier, char[] secret) {
		return true;
	}

}
