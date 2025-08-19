package com.ddtt.auth;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.util.Date;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;

@Singleton
public class EmailVerificationService {

    private final String secret;
    private final long expirationMs;

    public EmailVerificationService(
        @Value("${app.security.email-verification.secret}") String secret,
        @Value("${app.security.email-verification.expiration}") long expirationMs
    ) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) throws Exception {
        JWSSigner signer = new MACSigner(secret);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(email)
                .expirationTime(new Date(System.currentTimeMillis() + expirationMs))
                .issueTime(new Date())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
        );

        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public String verifyToken(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(secret);

        if (!signedJWT.verify(verifier)) {
            throw new SecurityException("Token signature invalid");
        }

        Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
        if (expiration.before(new Date())) {
            throw new SecurityException("Token expired");
        }

        return signedJWT.getJWTClaimsSet().getSubject();
    }
}
