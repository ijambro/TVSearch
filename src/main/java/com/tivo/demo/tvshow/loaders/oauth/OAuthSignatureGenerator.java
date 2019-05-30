package com.tivo.demo.tvshow.loaders.oauth;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

//import org.apache.commons.codec.binary.Base64;
import java.util.Base64;
import java.util.Base64.Encoder;

/**
 * The implementation of the OAuth 1.0 signature process.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.4">RFC5849 OAuth
 *      1.0 Signature</a>
 */
public class OAuthSignatureGenerator {

	private static final String HMAC_SHA1 = "HmacSHA1";
	private static final String ENC = "UTF-8";
	private String nonce;
	private String timestamp;

	public OAuthSignatureGenerator() {
		nonce = String.valueOf((int) (Math.random() * 100000000));
		timestamp = String.valueOf(System.currentTimeMillis() / 1000);
	}

	/**
	 * Build the OAuth 1.0 HTTP Authorization Header.
	 * 
	 * @param apiKey
	 *            - The public API key that authorizes your access to Metadata Cloud
	 *            Services, provided by TiVo when you signed up for the service.
	 * @param signature
	 *            - The string result of the
	 *            {@link #generateSignature(String, String, String, String)} method.
	 * @return
	 * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.5.1">RFC5849
	 *      Authorization Header</a>
	 */
	public String buildAuthorizationHeader(String apiKey, String signature) {
		StringBuilder oAuthHeader = new StringBuilder();
		oAuthHeader.append("OAuth ");
		oAuthHeader.append(String.format("oauth_consumer_key=%s", apiKey));
		oAuthHeader.append(", ");
		oAuthHeader.append("oauth_version=1.0");
		oAuthHeader.append(", ");
		oAuthHeader.append("oauth_signature_method=HMAC-SHA1");
		oAuthHeader.append(", ");
		oAuthHeader.append(String.format("oauth_timestamp=%s", timestamp));
		oAuthHeader.append(", ");
		oAuthHeader.append(String.format("oauth_nonce=%s", nonce));
		oAuthHeader.append(", ");

		try {
			oAuthHeader.append("oauth_signature=" + percentEncode(signature));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return oAuthHeader.toString();
	}

	/**
	 * Generate the OAuth 1.0 signature.
	 * 
	 * @param httpMethod
	 *            - The HTTP request method in uppercase.
	 * @param url
	 *            - The HTTP request URL.
	 * @param consumerKey
	 *            - The public API key that authorizes your access to Metadata Cloud
	 *            Services, provided by TiVo when you signed up for the service.
	 * @param consumerSecret
	 *            - The secret is used to generate the value of `oauth_signature`
	 *            and should never be transmitted.
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.4">RFC5849
	 *      Signature</a>
	 * @see <a href=
	 *      "http://oauth.googlecode.com/svn/code/javascript/example/signature.html">
	 *      OAuth 1.0 Signature Generator</a>
	 */
	public String generateSignature(String httpMethod, String url, String consumerKey, String consumerSecret)
			throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
		/**
		 * Base String URI
		 * 
		 * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.4.1.2">RFC5849
		 *      Base String URI</a>
		 */
		String baseURL = new String(url);
		int index = url.indexOf('?');
		if (index != -1) {
			baseURL = url.substring(0, url.indexOf('?')); // .toLowerCase(); only the
															// scheme and host should be
															// lowercase
		}

		/**
		 * String Construction
		 * 
		 * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.4.1.1">RFC5849
		 *      String Construction</a> The signature base string has three parts,
		 *      connected by "&": 1) HTTP Method, 2) Percent-encoded base URL, and 3)
		 *      Percent-encoded request parameters.
		 */
//		Base64 base64 = new Base64();
		Encoder base64 = Base64.getEncoder();
		
		StringBuilder signatureBaseString = new StringBuilder();
		signatureBaseString.append(httpMethod);
		signatureBaseString.append("&");
		signatureBaseString.append(percentEncode(baseURL));
		signatureBaseString.append("&");
		signatureBaseString.append(generateRequestParameters(url, consumerKey));

		/**
		 * Generate the HMAC_SHA1 signature
		 * 
		 * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.4.2">RFC5849
		 *      HMAC-SHA1</a>
		 */
		byte[] keyBytes = (consumerSecret + "&").getBytes(ENC);
		SecretKey key = new SecretKeySpec(keyBytes, HMAC_SHA1);
		Mac mac = Mac.getInstance(HMAC_SHA1);
		mac.init(key);

		// encode it, base64 it, change it to string and return.
		String signature = new String(base64.encode(mac.doFinal(signatureBaseString.toString().getBytes(ENC))), ENC)
				.trim();
		return signature;
	}

	/**
	 * OAuth 1.0 percent encode.
	 * 
	 * @param s
	 *            The string to be encoded.
	 * @return
	 * @throws UnsupportedEncodingException
	 * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.6">RFC5849 OAuth
	 *      1.0 Percent Encoding</a>
	 */
	private String percentEncode(String s) throws UnsupportedEncodingException {
		return URLEncoder.encode(s, ENC).replace("+", "%20").replace("%7E", "~").replace("%2E", ".").replace("%2D", "-")
				.replace("%5F", "_");
	}

	/**
	 * Generate the request parameter string.
	 * 
	 * @param url
	 *            - The HTTP request URL.
	 * @param consumerKey
	 *            - The public API key that authorizes your access to Metadata Cloud
	 *            Services, provided by TiVo when you signed up for the service.
	 * @return
	 * @throws UnsupportedEncodingException
	 * @see <a href="http://tools.ietf.org/html/rfc5849#section-3.4.1.3">RFC5849
	 *      Request Parameters</a>
	 */
	private String generateRequestParameters(String url, String consumerKey) throws UnsupportedEncodingException {

		// Extract request parameters from the URL and store in a sorted collection.
		Map<String, String> paramMap = new TreeMap<String, String>();

		Map<String, String> param0s = parse(url);
		for (String key : param0s.keySet()) {
			paramMap.put(key, percentEncode(param0s.get(key)));
		}

		// Add the OAuth request parameters to the collection of parameters.
		paramMap.put("oauth_consumer_key", consumerKey);
		paramMap.put("oauth_nonce", nonce);
		paramMap.put("oauth_signature_method", "HMAC-SHA1");
		paramMap.put("oauth_timestamp", timestamp);
		paramMap.put("oauth_version", "1.0");

		// Build the request parameters string.
		StringBuilder paramStringBuilder = new StringBuilder();
		for (Map.Entry<String, String> entry : paramMap.entrySet()) {
			paramStringBuilder.append(entry.getKey());
			paramStringBuilder.append("=");
			paramStringBuilder.append(entry.getValue());
			paramStringBuilder.append("&");
		}
		paramStringBuilder.deleteCharAt(paramStringBuilder.length() - 1);
		String params = paramStringBuilder.toString();
		return percentEncode(params);
	}

	private final String QUERY_SEPARATOR = "\\?";
	private final String PARAMETER_SEPARATOR = "&";
	private final String NAME_VALUE_SEPARATOR = "=";

	public Map<String, String> parse(String uri) throws UnsupportedEncodingException {
		Map<String, String> result = Collections.emptyMap();
		String[] uriParts = uri.split(QUERY_SEPARATOR);
		if (uriParts.length > 1) {
			result = new HashMap<String, String>();
			parse(result, new Scanner(uriParts[1]));
		}
		return result;
	}

	public void parse(final Map<String, String> parameters, final Scanner scanner) throws UnsupportedEncodingException {
		scanner.useDelimiter(PARAMETER_SEPARATOR);
		while (scanner.hasNext()) {
			final String[] nameValue = scanner.next().split(NAME_VALUE_SEPARATOR, 2);
			if (nameValue.length == 0)
				throw new IllegalArgumentException("bad parameter");

			final String name = URLDecoder.decode(nameValue[0], ENC);
			String value = null;
			if (nameValue.length == 2)
				value = URLDecoder.decode(nameValue[1], ENC);
			parameters.put(name, value);
		}
	}

	/**
	 * @see #nonce
	 */
	public String getNonce() {
		return nonce;
	}

	/**
	 * To override the default nonce created in constructor
	 * 
	 * @see #nonce
	 */
	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	/**
	 * @see #timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * To override the default timestamp set in constructor
	 * 
	 * @see #timestamp
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public static void main(String[] args) {
		try {
			String httpMethod = "GET", url = "http://sample.url.com/query?samplequery=sample value",
					consumerKey = "key", consumerSecret = "secret";

			OAuthSignatureGenerator oauth = new OAuthSignatureGenerator();
			// overriding nonce and timestamp
			oauth.setTimestamp("1458478319");
			oauth.setNonce("78834590");

			String signature = oauth.generateSignature(httpMethod, url, consumerKey, consumerSecret);
			String oauthHeader = oauth.buildAuthorizationHeader(consumerKey, signature);

			System.out.println("Authorization Header:\n" + oauthHeader);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
