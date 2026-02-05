package com.something.kodex_backend.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Integer> {

  @Query("""
  select t from Token t inner join User u on t.user.id = u.id
  where t.revoked = false and t.type = 'JWT_REFRESH'
""")
  // currently I'm only going to store jwt refresh tokens
  // TODO: handle logout by storing access token as well
  Optional<List<Token>> findAllValidJWTRefreshTokensByUserId(Integer userId);

  Optional<Token> findByValue(String value);

}
