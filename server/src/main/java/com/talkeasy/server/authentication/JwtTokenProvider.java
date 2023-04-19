package com.talkeasy.server.authentication;

import com.talkeasy.server.domain.Member;
import com.talkeasy.server.service.user.CustomUserDetailService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
@Log4j2
@Configuration
@RequiredArgsConstructor
@PropertySource("classpath:/application.yml")
public class JwtTokenProvider {
    @Value("${spring.app.auth.token.secret-key}")
    private String SECRET_KEY;
    private Long ACCESS_TOKEN_EXPIRE_LENGTH = 1000L*60*60000;
    private Long REFRESH_TOKEN_EXPIRE_LENGTH = 1000L*60*60*24*7000;
    private final CustomUserDetailService userDetailsService;
    @PostConstruct
    protected void init() {
        this.SECRET_KEY = Base64.getEncoder().encodeToString(SECRET_KEY.getBytes());
    }

    public String createAccessToken(Member member) {
        return createToken(member, ACCESS_TOKEN_EXPIRE_LENGTH);
    }

    public String createRefreshToken(Member member) {
        return createToken(member, REFRESH_TOKEN_EXPIRE_LENGTH);
    }

    public String createToken(Member member, long expireLength) {
        Claims claims = Jwts.claims().setSubject(member.getEmail()); // payload부분에 들어갈 정보 조각
        claims.put("username", member.getEmail());
        Date now = new Date();
        Date validity = new Date(now.getTime() + expireLength);
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
//        Object SignatureAlgorithm = io.jsonwebtoken.SignatureAlgorithm.HS512;
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS512)
                .compact();

    }

    public boolean validateToken(String token) { // 토큰 유효성 검사
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY.getBytes())
                    .build()
                    .parseClaimsJws(token);
            return !claimsJws.getBody().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) { // 토큰을 파싱하여 Authentication 객체 생성
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUserIdentifier(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUserIdentifier(String token){
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String resolveToken(HttpServletRequest request) { // 헤더로 부터 토큰 얻어옴
        return request.getHeader("X-AUTH-TOKEN");
    }
}
