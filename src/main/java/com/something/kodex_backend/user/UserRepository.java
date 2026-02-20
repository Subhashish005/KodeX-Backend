package com.something.kodex_backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

  Optional<User> findByUsername(String username);
  Optional<User> findByEmail(String email);

  @Query(value = """
  SELECT EXISTS(SELECT 1 FROM t_user WHERE id=:id) AS user_exists
""", nativeQuery = true
  )
  Optional<Boolean> checkIfUserExistsById(Integer id);
}