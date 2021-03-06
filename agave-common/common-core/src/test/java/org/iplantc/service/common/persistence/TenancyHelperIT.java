package org.iplantc.service.common.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.iplantc.service.common.auth.JWTClient;
import org.iplantc.service.common.dao.TenantDao;
import org.iplantc.service.common.exceptions.TenantException;
import org.iplantc.service.common.model.Tenant;
import org.testng.Assert;
import org.testng.annotations.*;

import java.util.Random;

@Test(groups={"integration"})
public class TenancyHelperIT {
	private static final Logger log = Logger.getLogger(TenancyHelperIT.class);

	private TenantDao dao;
	private Tenant tenant;
	
	private Tenant createTenant(String tenantCode)
	{
		Tenant tenant = new Tenant();
		tenant.setBaseUrl("https://api.example.com");
		tenant.setTenantCode(tenantCode);
		tenant.setContactEmail("foo@example.com");
		tenant.setContactName("Foo Bar");
		tenant.setStatus("ACTIVE");
		return tenant;
	}

	/**
	 * Creates a nonce for use as the token by generating an md5 hash of the
	 * salt, current timestamp, and a random number.
	 *
	 * @param salt
	 * @return md5 hash of the adjusted salt
	 */
	private String createNonce(String salt) {
		String digestMessage = salt + System.currentTimeMillis() + new Random().nextInt();
		return DigestUtils.md5Hex(digestMessage);
	}

	@BeforeClass
	public void beforeClass() throws TenantException {
		dao = new TenantDao();

		String tenantCode = createNonce(this.getClass().getName());
		tenant = createTenant(tenantCode);
		dao.persist(tenant);
	}
	
	@AfterClass
	public void afterClass() {
		try {
			dao.delete(tenant);
		} catch (TenantException e) {
			log.error("Failed to clean up test tenant after test");
		}
	}
	
	@BeforeMethod
	public void beforeMethod() 
	{
		String username = "testuser";
		StringBuilder builder = new StringBuilder();
		builder.append("eyJ0eXAiOiJKV1QiLCJhbGciOiJTSEEyNTZ3aXRoUlNBIiwieDV0IjoiTm1KbU9HVXhNelpsWWpNMlpEUmhOVFpsWVRBMVl6ZGhaVFJpT1dFME5XSTJNMkptT1RjMVpBPT0ifQ==.");
		builder.append(Base64.encode(new ObjectMapper().createObjectNode()
			.put("iss","wso2.org/products/am")
			.put("exp",  (System.currentTimeMillis() / 1000 + 86400))
			.put("http://wso2.org/claims/subscriber", username)
			.put("http://wso2.org/claims/applicationid", "5")
			.put("http://wso2.org/claims/applicationname", "DefaultApplication")
			.put("http://wso2.org/claims/applicationtier", "Unlimited")
			.put("http://wso2.org/claims/apicontext", "*")
			.put("http://wso2.org/claims/version", "2.0")
			.put("http://wso2.org/claims/tier", "Unlimited")
			.put("http://wso2.org/claims/keytype", "PRODUCTION")
			.put("http://wso2.org/claims/usertype", "APPLICATION_USER")
			.put("http://wso2.org/claims/enduser", username)
			.put("http://wso2.org/claims/enduserTenantId", "-9999")
			.put("http://wso2.org/claims/emailaddress", tenant.getContactEmail())
			.put("http://wso2.org/claims/fullname", tenant.getContactName())
			.put("http://wso2.org/claims/givenname", "Dev")
			.put("http://wso2.org/claims/lastname", "User")
			.put("http://wso2.org/claims/primaryChallengeQuestion", "N/A")
			.put("http://wso2.org/claims/role", "Internal/everyone")
			.put("http://wso2.org/claims/title", "N/A").toString()));
		
		builder.append(".FA6GZjrB6mOdpEkdIQL/p2Hcqdo2QRkg/ugBbal8wQt6DCBb1gC6wPDoAenLIOc+yDorHPAgRJeLyt2DutNrKRFv6czq1wz7008DrdLOtbT4EKI96+mXJNQuxrpuU9lDZmD4af/HJYZ7HXg3Hc05+qDJ+JdYHfxENMi54fXWrxs=");
		
		JWTClient.parse(builder.toString(), tenant.getTenantCode());
	}
	
	

	@DataProvider(name = "resolveURLToCurrentTenantProvider")
	public Object[][] resolveURLToCurrentTenantProvider() 
	{
		String suffix = "apps/v2";
		return new Object[][] {
			new Object[] { "http://foo.com/" + suffix, tenant.getBaseUrl() + suffix, "Basic tenant domain replacement failed" },
			new Object[] { "https://foo.com/" + suffix, tenant.getBaseUrl() + suffix, "URL scheme was not replaced with tenant scheme" },
			new Object[] { "https://foo.com:8080/" + suffix, tenant.getBaseUrl() + suffix, "URL port was not replaced with tenant url" },
			new Object[] { "https://foo.com/" + "/" + suffix, tenant.getBaseUrl() + "/" + suffix, "Absolute path in URL was not preserved" },
			new Object[] { "https://foo.com/" + suffix + "?foo=bar", tenant.getBaseUrl() + suffix + "?foo=bar", "Query string was not preserved" }
		};
	}

	@Test(dataProvider = "resolveURLToCurrentTenantProvider")
	public void resolveURLToCurrentTenant(String originalUrl, String expectedUrl, String message) 
	{
		String resolvedUrl = TenancyHelper.resolveURLToCurrentTenant(originalUrl);
		
		Assert.assertEquals(resolvedUrl, expectedUrl, message);
	}
}
