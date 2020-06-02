package io.vertx.ext.web.handler.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.agaveplatform.service.transfers.util.AuthHelper;
import org.apache.commons.lang.StringUtils;

import javax.naming.AuthenticationException;
import java.util.List;
import java.util.Map;

public class AgaveJWTAuthHandlerImpl extends AuthorizationAuthHandler implements JWTAuthHandler {
    private final String skip;
    private final JsonObject options;

    @Override
    public void authorize(User user, Handler<AsyncResult<Void>> handler) {
        super.authorize(user, handler);
    }

    public AgaveJWTAuthHandlerImpl(JWTAuth authProvider) {
        this(authProvider, (String)null);
    }

    public AgaveJWTAuthHandlerImpl(JWTAuth authProvider, String skip) {
        super(authProvider, AuthorizationAuthHandler.Type.BEARER);
        this.skip = skip;
        this.options = new JsonObject();
    }

    public JWTAuthHandler setAudience(List<String> audience) {
        this.options.put("audience", new JsonArray(audience));
        return this;
    }

    public JWTAuthHandler setIssuer(String issuer) {
        this.options.put("issuer", issuer);
        return this;
    }

    public JWTAuthHandler setIgnoreExpiration(boolean ignoreExpiration) {
        this.options.put("ignoreExpiration", ignoreExpiration);
        return this;
    }

    /**
     * Parses the map returned from the call to {@link #parseMultiTenantAuthorization(RoutingContext, boolean, Handler)}
     * and passes the info to the handler for use initializing the {@link Wso2JwtUser} that will be attached to the
     * {@link RoutingContext} for the rest of the request lifecycle.
     *
     * @param context the routing context for this request
     * @param handler the handler to call with the result of this async call.
     */
    public void parseCredentials(RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
        this.parseMultiTenantAuthorization(context, false, (parseAuthorization) -> {
            if (parseAuthorization.failed()) {
                handler.handle(Future.failedFuture(parseAuthorization.cause()));
            } else {
                Map<String,String> map = parseAuthorization.result();
                JsonObject futureResponse = new JsonObject()
                        .put("jwt", map.get("jwt"))
                        .put("tenantId", map.get("tenantId"))
                        .put("rawTenantId", map.get("rawTenantId"))
                        .put("options", this.options);
                handler.handle(Future.succeededFuture(futureResponse));
            }
        });
    }

    /**
     * Parses the headers and locates the first auth header for a tenant. This is generally one which begins with
     * {@code x-jwt-assertion-}.
     *
     * @param ctx the routing context for this request
     * @param optional is auth optional.
     * @param handler the handler to call with the result of this async call.
     */
    protected void parseMultiTenantAuthorization(RoutingContext ctx, boolean optional, Handler<AsyncResult<Map<String,String>>> handler) {
          HttpServerRequest request = ctx.request();
        List<Map.Entry<String,String>> headers = request.headers().entries();
        String authHeader = AuthHelper.getAuthHeader(headers);
        if (authHeader == null) {
            handler.handle(Future.failedFuture(FORBIDDEN));
            return;
        }

        String authorization = request.headers().get(authHeader);
        if (authorization == null) {
            if (optional) {
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(UNAUTHORIZED));
            }
        } else {
            try {
                int idx = authorization.indexOf(32);
                if (idx > 0) {
                    handler.handle(Future.failedFuture(BAD_REQUEST));
                    return;
                }

                String rawTenantId = AuthHelper.getTenantIdFromAuthHeader(authHeader);
                String tenantId = StringUtils.replaceChars(rawTenantId, '_', '.');
                tenantId = StringUtils.replaceChars(tenantId, '-', '.');

                Map<String,String> map = Map.of(
                        "bearer", request.headers().contains(HttpHeaders.AUTHORIZATION) ? request.headers().get(HttpHeaders.AUTHORIZATION) : "",
                        "jwt", authorization,
                        "rawTenantId", rawTenantId,
                        "tenantId", tenantId);

                handler.handle(Future.succeededFuture(map));
            } catch (RuntimeException var7) {
                handler.handle(Future.failedFuture(var7));
            }
        }
    }

    protected String authenticateHeader(RoutingContext context) {
        return "Bearer";
    }

    static enum Type {
        BASIC("Basic"),
        DIGEST("Digest"),
        BEARER("Bearer"),
        HOBA("HOBA"),
        MUTUAL("Mutual"),
        NEGOTIATE("Negotiate"),
        OAUTH("OAuth"),
        SCRAM_SHA_1("SCRAM-SHA-1"),
        SCRAM_SHA_256("SCRAM-SHA-256");

        private final String label;

        private Type(String label) {
            this.label = label;
        }

        public boolean is(String other) {
            return this.label.equalsIgnoreCase(other);
        }
    }
}
