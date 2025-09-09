package com.ddtt.utils;

import com.ddtt.services.AccountService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.util.Date;

@Singleton
public class JwtUtils {

    private final String emailSecret;
    private final long emailExpirationMs;
    private final String loginSecret;
    private final long loginExpirationMs;
    private final long refreshExpirationMs;


    public JwtUtils(
        @Value("${app.verification.email.secret}") String emailSecret,
        @Value("${app.verification.email.expiration}") long emailExpirationMs,
        @Value("${app.verification.login.secret}") String loginSecret,
        @Value("${app.verification.login.expiration}") long loginExpirationMs,
        @Value("${app.verification.login.refreshExpiration}") long refreshExpirationMs
    ) {
        this.emailSecret = emailSecret;
        this.emailExpirationMs = emailExpirationMs;
        this.loginSecret = loginSecret;
        this.loginExpirationMs = loginExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateTokenForEmail(String email) throws Exception {
        JWSSigner signer = new MACSigner(emailSecret);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(email)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public void verifyEmailToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(emailSecret);

        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("Token signature invalid");
        }
    }

    public String generateTokenForLogin(String email, int accountId) throws Exception {
        JWSSigner signer = new MACSigner(loginSecret);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(email)
                .claim("accountId", accountId)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public int verifyLoginToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(loginSecret);

        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("Token signature invalid");
        }

        return signedJWT.getJWTClaimsSet().getIntegerClaim("accountId");
    }

    public String generateRefreshToken(String email) throws Exception {
        JWSSigner signer = new MACSigner(loginSecret);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(email)
                .claim("type", "refresh")
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
        );
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public String verifyRefreshToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(loginSecret);

        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("Token signature invalid");
        }
        if (!"refresh".equals(signedJWT.getJWTClaimsSet().getStringClaim("type"))) {
            throw new SecurityException("Invalid token type");
        }
        return signedJWT.getJWTClaimsSet().getSubject();
    }

    public long getLoginExpirationMs() {
        return loginExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

}
